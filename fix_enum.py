import os
file_path = 'server/movie-service/src/test/java/com/lorafilm/movie/showtime/repository/ShowtimeRepositoryIntegrationTest.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('st.setStatus(ShowtimeStatus.SCHEDULED);', 'st.setStatus(ShowtimeStatus.DRAFT);')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed enum')
