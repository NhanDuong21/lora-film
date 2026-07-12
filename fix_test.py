import os
import re

file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/validation/ShowtimeValidationServiceImplTest.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add validOpHour setup
setup_pattern = r'startTime = startZdt.toInstant\(\);(.*?)context = new ShowtimeValidationContext\('
setup_replacement = r'''startTime = startZdt.toInstant();
        
        validOpHour = new CinemaOperatingHour();
        validOpHour.setDayOfWeek(startZdt.getDayOfWeek().getValue());
        validOpHour.setOpenTime(LocalTime.of(8, 0));
        validOpHour.setCloseTime(LocalTime.of(23, 0));
        validOpHour.setIsClosed(false);

\1context = new ShowtimeValidationContext('''
content = re.sub(setup_pattern, setup_replacement, content, flags=re.DOTALL)

# Add validOpHour to class
content = content.replace('private ShowtimeValidationContext context;', 'private ShowtimeValidationContext context;\n    private CinemaOperatingHour validOpHour;')

# Replace when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(Collections.emptyList());
# with when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
content = content.replace(
    'when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(Collections.emptyList());',
    'when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));'
)

# Remove old validateForScheduling_durationTooShort_shouldThrowException
duration_test_pattern = r'    @Test\s+void validateForScheduling_durationTooShort_shouldThrowException\(\) \{.*?\n    \}\n'
content = re.sub(duration_test_pattern, '', content, flags=re.DOTALL)

# Insert new duration tests before validateForScheduling_outsideOperatingHours_shouldThrowException
new_duration_tests = '''    @Test
    void validateForScheduling_invalidDuration_shouldThrowException() {
        Integer[] invalidDurations = {null, 0, -1};
        for (Integer dur : invalidDurations) {
            movie.setDurationMinutes(dur);
            ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, null);
            BusinessException ex = assertThrows(BusinessException.class, 
                    () -> showtimeValidationService.validateScheduling(ctx));
            assertEquals(ErrorCode.INVALID_MOVIE_DURATION, ex.getErrorCode());
        }
    }

    @Test
    void validateForScheduling_validDuration_shouldPass() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any())).thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any())).thenReturn(false);
        when(showtimeRepository.findPotentialOverlaps(eq(auditorium.getId()), any(), any())).thenReturn(Collections.emptyList());

        Integer[] validDurations = {1, 29, 30, 138};
        for (Integer dur : validDurations) {
            movie.setDurationMinutes(dur);
            ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, null);
            assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(ctx));
        }
    }
'''
content = content.replace('    @Test\n    void validateForScheduling_outsideOperatingHours_shouldThrowException()', new_duration_tests + '\n    @Test\n    void validateForScheduling_outsideOperatingHours_shouldThrowException()')


# Replace outsideOperatingHours test to check overnight
overnight_test = '''    @Test
    void validateForScheduling_missingOperatingHours_shouldThrowException() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(Collections.emptyList());
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.CINEMA_OPERATING_HOURS_NOT_CONFIGURED, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_outsideOperatingHours_shouldThrowException() {
        validOpHour.setOpenTime(LocalTime.of(12, 0));
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_overnightOperatingHours_shouldPass() {
        // Monday 08:00 to 02:00
        CinemaOperatingHour mondayOpHour = new CinemaOperatingHour();
        mondayOpHour.setDayOfWeek(1); // Monday
        mondayOpHour.setOpenTime(LocalTime.of(8, 0));
        mondayOpHour.setCloseTime(LocalTime.of(2, 0));
        mondayOpHour.setIsClosed(false);

        // Showtime on Tuesday 01:00 AM
        ZonedDateTime startZdt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.TUESDAY))
                .withHour(1).withMinute(0).withSecond(0).withNano(0);
        
        movie.setDurationMinutes(30); // Ends at 01:30 AM
        ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startZdt.toInstant(), null);
        
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(mondayOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any())).thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any())).thenReturn(false);
        when(showtimeRepository.findPotentialOverlaps(eq(auditorium.getId()), any(), any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(ctx));
    }
'''
outside_op_pattern = r'    @Test\s+void validateForScheduling_outsideOperatingHours_shouldThrowException\(\) \{.*?\n    \}\n'
content = re.sub(outside_op_pattern, overnight_test, content, flags=re.DOTALL)


with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated successfully")
