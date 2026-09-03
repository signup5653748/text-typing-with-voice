import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

# Remove Retrofit, OkHttp, Moshi
content = re.sub(r'\s*implementation\(libs\.converter\.moshi\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.logging\.interceptor\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.moshi\.kotlin\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.okhttp\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.retrofit\)\n', '\n', content)
content = re.sub(r'\s*\"ksp\"\(libs\.moshi\.kotlin\.codegen\)\n', '\n', content)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
