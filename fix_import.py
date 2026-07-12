import os
file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/service/ShowtimeQueryServiceImplTest.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import com.lorafilm.movie.showtime.dto.response.ShowtimeDto;', 'import com.lorafilm.movie.showtime.dto.ShowtimeDto;')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed import')
