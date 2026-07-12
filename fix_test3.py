import os
import re
file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/validation/ShowtimeValidationServiceImplTest.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace any new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, null)
# or new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime)
# with new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, endTime, null)
# wait, for invalidDuration, there's no endTime? We can just pass startTime.plusSeconds(30*60).
# Actually, the user says "null, 0, -1" for duration, we can just pass an arbitrary endTime since it's not checked.

content = re.sub(r'new ShowtimeValidationContext\([^)]*\)', 'new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, startTime.plusSeconds(1800), null)', content)

# But wait, startZdt.toInstant() is used in overnight test.
# Let's do a more precise replace.

content = re.sub(r'ShowtimeValidationContext ctx = new ShowtimeValidationContext\(movie, movieVersion, cinema, auditorium, startZdt\.toInstant\(\), null\);',
                 r'ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startZdt.toInstant(), startZdt.toInstant().plusSeconds(1800), null);', content)

content = re.sub(r'ShowtimeValidationContext ctx = new ShowtimeValidationContext\(movie, movieVersion, cinema, auditorium, startTime, null\);',
                 r'ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, startTime.plusSeconds(1800), null);', content)

content = re.sub(r'context = new ShowtimeValidationContext\(movie, movieVersion, cinema, auditorium, startTime\);',
                 r'context = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, startTime.plusSeconds(1800), null);', content)


with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed constructor arguments completely')
