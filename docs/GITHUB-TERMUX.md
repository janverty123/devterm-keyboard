# Build and maintain DevTerm Keyboard using only Android

The repository is already created at https://github.com/janverty123/devterm-keyboard.

## Clone in Termux

Run:

    pkg update
    pkg install git gh
    gh auth login
    gh auth setup-git
    git clone https://github.com/janverty123/devterm-keyboard.git
    cd devterm-keyboard

Sign in to GitHub using the browser. Never share your authentication token in chat.

## Build APK without a laptop
1. Open the repository in GitHub.
2. Select Actions, then Android debug APK.
3. Choose Run workflow (or push a commit to main to trigger it).
4. After a successful run, download the devterm-keyboard-debug artifact.
5. Extract the ZIP and install app-debug.apk on Android, confirming Android's install warning for your trusted build.

## Make future changes
Edit code locally in Termux/Acode, then use:

    git add .
    git commit -m "Describe your change"
    git push origin main

Android's third-party keyboard warning is expected; all keyboards have access to entered text.
