# Contributing to Dahua NMEA Recorder

Thank you for your interest in contributing to Dahua NMEA Recorder! This document provides guidelines for contributing to the project.

## How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with the following information:

1. **Title**: Clear and descriptive title
2. **Description**: Detailed description of the bug
3. **Steps to Reproduce**: Step-by-step instructions
4. **Expected Behavior**: What should happen
5. **Actual Behavior**: What actually happens
6. **Environment**:
   - Device model (e.g., Dahua MPT230)
   - Android version
   - App version
   - GPS conditions
7. **Logs**: Include relevant logcat output if possible
8. **Screenshots**: If applicable

### Suggesting Features

Feature suggestions are welcome! Please create an issue with:

1. **Title**: Clear feature name
2. **Description**: Detailed explanation of the feature
3. **Use Case**: Why this feature is needed
4. **Mockups**: UI mockups if applicable
5. **Priority**: Low/Medium/High

### Code Contributions

#### Before You Start

1. Fork the repository
2. Create a new branch for your feature/fix
3. Follow the existing code style
4. Write clear commit messages
5. Test your changes thoroughly

#### Code Style

**Kotlin Style Guidelines**:
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions small and focused
- Use proper indentation (4 spaces)

**File Organization**:
```kotlin
// 1. Package declaration
package com.dahua.nmea

// 2. Imports (sorted alphabetically)
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// 3. Class declaration
class MyClass {
    // 4. Companion object
    companion object {
        private const val TAG = "MyClass"
    }
    
    // 5. Properties
    private var myProperty: String = ""
    
    // 6. Lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    
    // 7. Public methods
    fun publicMethod() {}
    
    // 8. Private methods
    private fun privateMethod() {}
}
```

#### Pull Request Process

1. **Update Documentation**: Update README.md, comments, etc.
2. **Add Tests**: If applicable
3. **Update CHANGELOG.md**: Add your changes
4. **Create Pull Request**:
   - Clear title
   - Description of changes
   - Link to related issues
   - Screenshots/videos if UI changes
5. **Wait for Review**: Address feedback promptly
6. **Merge**: After approval

#### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Formatting, missing semicolons, etc.
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance

**Example**:
```
feat(gps): Add GPS accuracy filter

Add user setting to filter GPS points by accuracy threshold.
Only points with accuracy < threshold will be saved to NMEA.

Closes #123
```

### Development Setup

1. **Prerequisites**:
   - Android Studio Arctic Fox or newer
   - JDK 8+
   - Android SDK API 30+
   - Git

2. **Clone Repository**:
   ```bash
   git clone https://github.com/dahua/nmea-recorder.git
   cd nmea-recorder
   ```

3. **Open in Android Studio**:
   - File → Open → Select project folder
   - Wait for Gradle sync

4. **Build**:
   ```bash
   ./gradlew build
   ```

5. **Run Tests**:
   ```bash
   ./gradlew test
   ```

### Testing

Before submitting a pull request:

1. **Unit Tests**: Write tests for new functionality
2. **Manual Testing**: Test on actual device
3. **Test Cases**:
   - [ ] App starts without crash
   - [ ] Permissions work correctly
   - [ ] Recording starts/stops properly
   - [ ] GPS tracking works
   - [ ] Files are created correctly
   - [ ] NMEA format is valid
   - [ ] UI updates correctly
   - [ ] No memory leaks
   - [ ] Battery usage is acceptable

### Code Review Checklist

Reviewers will check:

- [ ] Code follows style guidelines
- [ ] No hardcoded values (use resources)
- [ ] Proper error handling
- [ ] Memory leaks prevented
- [ ] Permissions handled correctly
- [ ] UI is responsive
- [ ] Documentation updated
- [ ] CHANGELOG updated
- [ ] No unnecessary dependencies
- [ ] Code is well-commented
- [ ] Backward compatibility maintained

## Project Structure

Understanding the project structure:

```
app/src/main/
├── java/com/dahua/nmea/
│   ├── MainActivity.kt              # Main UI
│   ├── service/
│   │   └── RecordingService.kt      # Recording logic
│   └── utils/
│       ├── FileManager.kt           # File operations
│       ├── GpsTracker.kt            # GPS handling
│       ├── NmeaGenerator.kt         # NMEA conversion
│       └── UsbTransferHelper.kt     # USB transfer
├── res/
│   ├── layout/                      # XML layouts
│   ├── drawable/                    # Icons, shapes
│   ├── values/                      # Strings, colors, themes
│   └── xml/                         # Other XML configs
└── AndroidManifest.xml              # App configuration
```

## Getting Help

- **Issues**: Check existing issues first
- **Discussions**: Use GitHub Discussions for questions
- **Email**: support@dahuatech.com

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## Code of Conduct

### Our Standards

- Be respectful and inclusive
- Accept constructive criticism
- Focus on what's best for the project
- Show empathy towards others

### Unacceptable Behavior

- Harassment or discriminatory language
- Trolling or insulting comments
- Publishing others' private information
- Other unprofessional conduct

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project documentation

Thank you for contributing to Dahua NMEA Recorder! 🎉
