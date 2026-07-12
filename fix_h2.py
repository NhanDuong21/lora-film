import os
file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/repository/ShowtimeRepositoryIntegrationTest.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)', '')
content = content.replace('import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;', '')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Removed AutoConfigureTestDatabase')
