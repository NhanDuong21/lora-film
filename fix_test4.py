import os

file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/validation/ShowtimeValidationServiceImplTest.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix setUp duration
content = content.replace('movie.setEndDate(LocalDate.now().plusDays(5));', 'movie.setEndDate(LocalDate.now().plusDays(5));\n        movie.setDurationMinutes(120);')

# Fix overnight test line 233
bad_line = 'ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, startTime.plusSeconds(1800), null));'
good_line = 'ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startZdt.toInstant(), startZdt.toInstant().plusSeconds(1800), null);'
content = content.replace(bad_line, good_line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed test compilation and logic')
