# Replicant Memory Problem Reproduction

This repository contains a minimal reproduction of a potential memory issue in Replicant.

## Project Overview

This is a simple ClojureScript application that demonstrates a problem with the intl-tel-input component integration with Replicant. It's designed to help isolate and identify the issue.

## Technologies Used

- ClojureScript
- Shadow-CLJS
- Replicant (DOM rendering)
- DataScript (state management)
- TailwindCSS
- intl-tel-input (phone input component)

## Getting Started

1. Clone this repository
2. Install dependencies:
   ```
   npm install
   ```
3. Start the development server:
   ```
   npm run dev
   ```
4. In a separate terminal, compile the CSS:
   ```
   npm run tailwind
   ```
5. Open your browser to http://localhost:8080

## Issue Description

The repository demonstrates a potential issue with how Replicant handles the intl-tel-input component, particularly in relation to memory management.

### Steps to Reproduce

1. Open the application in a browser
2. Open the browser's developer console
3. Observe that at startup, the intl-tel-input instance is correctly created (you'll see a message like `iti = [object Object]` in the console)
4. Type a number in the phone input field
5. Observe the console log that shows `Updating node, params = {...}`
6. Notice that although the `:replicant/memory` key is present in the logged object, its value is `nil` instead of containing the intl-tel-input instance that was stored during the mount phase

This suggests that the memory stored during the `mount-intl-tel-input` function's execution (via the `remember` callback) is not being properly maintained when the component updates.

## License

This code is provided for demonstration purposes only.