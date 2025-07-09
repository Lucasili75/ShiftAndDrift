# 🏁 Shift & Drift - Web Edition

A modern Progressive Web App (PWA) racing game migrated from Android. Experience real-time multiplayer racing in your browser!

## 🎮 Features

- **🏁 Real-time Multiplayer Racing** - Compete with friends in live races
- **🎲 Grid Roll System** - Dice-based starting position determination  
- **🏎️ Car Customization** - Personalize your racing colors
- **📱 Cross-Platform** - Play on desktop, tablet, or mobile
- **🔔 Live Notifications** - Get notified of game updates
- **⚡ Progressive Web App** - Install like a native app

## 🚀 Quick Start

### Prerequisites
- Modern web browser (Chrome, Firefox, Safari, Edge)
- HTTP server for local development

### Installation

1. **Clone or download** this repository
2. **Navigate to webapp directory**:
   ```bash
   cd webapp
   ```
3. **Start a local server**:
   ```bash
   # Using Python
   python -m http.server 8000
   
   # Using Node.js
   npx serve
   
   # Using PHP  
   php -S localhost:8000
   ```
4. **Open browser** and go to `http://localhost:8000`

### Deploy to Production

**Vercel** (Recommended):
```bash
npx vercel --cwd webapp
```

**Netlify**:
- Drag and drop the `webapp` folder to [netlify.com/drop](https://netlify.com/drop)

**Firebase Hosting**:
```bash
firebase init hosting
firebase deploy
```

## 🎯 How to Play

1. **Setup Profile** - Enter your name and customize car colors
2. **Join/Create Game** - Start a new race or join existing ones  
3. **Roll for Grid** - Dice determine your starting position
4. **Race!** - Take turns moving based on gear selection
5. **Win!** - First to complete the laps wins

## 🛠️ Technology Stack

- **Frontend**: HTML5, CSS3, Modern JavaScript (ES6+)
- **Backend**: Firebase Realtime Database
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Server**: Express.js (for notifications)
- **Architecture**: Progressive Web App (PWA)

## 📁 Project Structure

```
webapp/
├── index.html              # Main application
├── styles.css              # All styling
├── app.js                  # Main app logic
├── game-logic.js           # Firebase & game management  
├── utils.js                # Helper functions
├── firebase-config.js      # Firebase configuration
├── firebase-messaging-sw.js # Service worker
├── manifest.json           # PWA manifest
└── assets/                 # Icons (to be added)
```

## 🔧 Development

### Architecture Overview

- **ShiftDriftApp**: Main application controller
- **GameLogic**: Firebase operations and game state management
- **Utils**: Utility functions and UI helpers

### Key Features Implemented

- ✅ Player profile management
- ✅ Real-time game lobby
- ✅ Multiplayer game creation/joining
- ✅ Grid roll system with dice animation
- ✅ Turn-based racing mechanics
- ✅ Live game state synchronization
- ✅ Push notifications
- ✅ Responsive mobile design

### Firebase Configuration

Update `firebase-config.js` with your Firebase project settings:

```javascript
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "your-project.firebaseapp.com", 
  databaseURL: "your-database-url",
  projectId: "your-project-id",
  storageBucket: "your-storage-bucket",
  messagingSenderId: "your-sender-id",
  appId: "your-app-id"
};
```

## 📱 Mobile Support

- **Responsive Design**: Adapts to all screen sizes
- **Touch Controls**: Optimized for mobile interaction
- **PWA Installation**: Can be installed as native app
- **Offline Support**: Basic offline functionality

## 🎨 Customization

### Themes
Modify CSS variables in `styles.css`:
```css
:root {
  --primary-color: #1976d2;
  --secondary-color: #ff5722;
  /* Add your brand colors */
}
```

### Game Rules
Adjust game mechanics in `game-logic.js`:
- Dice roll calculations
- Movement algorithms  
- Turn order logic
- Win conditions

## 🐛 Troubleshooting

### Common Issues

1. **Game won't load**
   - Check Firebase configuration
   - Verify internet connection
   - Check browser console for errors

2. **Notifications not working**  
   - Ensure HTTPS (required for FCM)
   - Check VAPID key configuration
   - Verify browser notification permissions

3. **Mobile layout issues**
   - Test on actual devices
   - Check responsive breakpoints
   - Verify touch event handling

### Debug Mode
Open browser console to see detailed logs and Firebase activity.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

This project is part of the Shift & Drift gaming ecosystem. See original Android app licensing.

## 🙏 Acknowledgments

- **Firebase** - Real-time database and messaging
- **Font Awesome** - UI icons
- **Original Android App** - Game concept and mechanics

---

**Ready to race? 🏁 Start your engines!**