import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

# Update release build type
content = re.sub(
    r'isMinifyEnabled = false',
    'isMinifyEnabled = true\n      isShrinkResources = true',
    content
)

# Remove unused plugins
content = re.sub(r'alias\(libs\.plugins\.google\.services\)\n', '', content)

# Remove googleServices block
content = re.sub(r'googleServices \{ missingGoogleServicesStrategy = MissingGoogleServicesStrategy\.WARN \}\n', '', content)

# Remove Firebase, Room, and unnecessary libraries from dependencies
content = re.sub(r'\s*implementation\(platform\(libs\.firebase\.bom\)\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.androidx\.room\.ktx\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.androidx\.room\.runtime\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.firebase\.ai\)\n', '\n', content)
content = re.sub(r'\s*implementation\(libs\.firebase\.appcheck\.recaptcha\)\n', '\n', content)
content = re.sub(r'\s*\"ksp\"\(libs\.androidx\.room\.compiler\)\n', '\n', content)
content = re.sub(r'\s*// Uncomment.*\n\s*// implementation\(libs\.firebase\.firestore\)\n', '\n', content)
content = re.sub(r'\s*// Uncomment ALL.*\n\s*// Sign-In.*\n\s*// implementation\(libs\.firebase\.auth\)\n\s*// implementation\(libs\.androidx\.credentials\)\n\s*// implementation\(libs\.androidx\.credentials\.play\.services\)\n\s*// implementation\(libs\.googleid\)\n', '\n', content)

# Also remove missingGoogleServicesStrategy import
content = re.sub(r'import com\.google\.gms\.googleservices\.GoogleServicesPlugin\.MissingGoogleServicesStrategy\n', '', content)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
