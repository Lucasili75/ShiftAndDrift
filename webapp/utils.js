/* ==============================================
   SHIFT & DRIFT - Utility Functions
   ============================================== */

// Color mapping object
const CAR_COLORS = {
    'Bianco': '#ffffff',
    'Rosso': '#f44336', 
    'Verde': '#4caf50',
    'Blu': '#2196f3',
    'Giallo': '#ffeb3b',
    'Nero': '#212121'
};

// Game status translations
const STATUS_TRANSLATIONS = {
    'waiting': 'In attesa',
    'rolling': 'Preparazione',
    'started': 'In corso',
    'finished': 'Terminata'
};

// Utility Functions
class Utils {
    
    // Generate a unique user ID
    static generateUID() {
        return 'web_' + Math.random().toString(36).substr(2, 9) + Date.now().toString(36);
    }
    
    // Generate a game code
    static generateGameCode() {
        return 'R' + Math.random().toString(36).substr(2, 5).toUpperCase();
    }
    
    // Get or create user ID
    static getUID() {
        let uid = localStorage.getItem('shiftdrift_uid');
        if (!uid) {
            uid = this.generateUID();
            localStorage.setItem('shiftdrift_uid', uid);
        }
        return uid;
    }
    
    // Local storage helpers
    static saveToStorage(key, value) {
        try {
            localStorage.setItem(`shiftdrift_${key}`, JSON.stringify(value));
        } catch (error) {
            console.error('Error saving to storage:', error);
        }
    }
    
    static getFromStorage(key, defaultValue = null) {
        try {
            const item = localStorage.getItem(`shiftdrift_${key}`);
            return item ? JSON.parse(item) : defaultValue;
        } catch (error) {
            console.error('Error reading from storage:', error);
            return defaultValue;
        }
    }
    
    // Player preferences
    static savePlayerPrefs(playerData) {
        this.saveToStorage('player', playerData);
    }
    
    static getPlayerPrefs() {
        return this.getFromStorage('player', {
            name: 'Player',
            carColorFront: 'Bianco',
            carColorBody: 'Bianco', 
            carColorRear: 'Bianco'
        });
    }
    
    // Status translation
    static getStatusText(status) {
        return STATUS_TRANSLATIONS[status] || 'Sconosciuto';
    }
    
    // Color helpers
    static getCarColorHex(colorName) {
        return CAR_COLORS[colorName] || '#ffffff';
    }
    
    static updateCarColors(frontColor, bodyColor, rearColor) {
        const frontEl = document.getElementById('frontColor');
        const bodyEl = document.getElementById('bodyColor'); 
        const rearEl = document.getElementById('rearColor');
        
        if (frontEl) frontEl.style.backgroundColor = this.getCarColorHex(frontColor);
        if (bodyEl) bodyEl.style.backgroundColor = this.getCarColorHex(bodyColor);
        if (rearEl) rearEl.style.backgroundColor = this.getCarColorHex(rearColor);
        
        // Update car visual preview
        const carFront = document.querySelector('.car-front');
        const carBody = document.querySelector('.car-body');
        const carRear = document.querySelector('.car-rear');
        
        if (carFront) carFront.style.backgroundColor = this.getCarColorHex(frontColor);
        if (carBody) carBody.style.backgroundColor = this.getCarColorHex(bodyColor);
        if (carRear) carRear.style.backgroundColor = this.getCarColorHex(rearColor);
    }
    
    // Format date/time
    static formatTime(timestamp) {
        if (!timestamp) return '';
        const date = new Date(timestamp);
        return date.toLocaleTimeString('it-IT', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }
    
    // Screen navigation
    static showScreen(screenId) {
        // Hide all screens
        document.querySelectorAll('.screen').forEach(screen => {
            screen.classList.remove('active');
        });
        
        // Show target screen
        const targetScreen = document.getElementById(screenId);
        if (targetScreen) {
            targetScreen.classList.add('active');
        }
    }
    
    // Loading overlay
    static showLoading(message = 'Caricamento...') {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.querySelector('p').textContent = message;
            overlay.classList.remove('hidden');
        }
    }
    
    static hideLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.classList.add('hidden');
        }
    }
    
    // Toast notifications
    static showToast(message, type = 'info', duration = 4000) {
        const container = document.getElementById('toastContainer');
        if (!container) return;
        
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div class="toast-content">
                <i class="fas fa-${this.getToastIcon(type)}"></i>
                <span>${message}</span>
            </div>
        `;
        
        container.appendChild(toast);
        
        // Auto remove
        setTimeout(() => {
            if (toast.parentNode) {
                toast.style.animation = 'slideOutRight 0.3s ease';
                setTimeout(() => {
                    if (toast.parentNode) {
                        toast.parentNode.removeChild(toast);
                    }
                }, 300);
            }
        }, duration);
    }
    
    static getToastIcon(type) {
        const icons = {
            'success': 'check-circle',
            'error': 'exclamation-circle',
            'warning': 'exclamation-triangle',
            'info': 'info-circle'
        };
        return icons[type] || 'info-circle';
    }
    
    // Modal helpers
    static showModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    }
    
    static hideModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('active');
            document.body.style.overflow = '';
        }
    }
    
    // Dice roll animation
    static rollDice(dice1El, dice2El, result1, result2, callback) {
        if (!dice1El || !dice2El) return;
        
        dice1El.classList.add('rolling');
        dice2El.classList.add('rolling');
        
        let iteration = 0;
        const maxIterations = 10;
        
        const animateInterval = setInterval(() => {
            dice1El.textContent = Math.floor(Math.random() * 6) + 1;
            dice2El.textContent = Math.floor(Math.random() * 6) + 1;
            
            iteration++;
            if (iteration >= maxIterations) {
                clearInterval(animateInterval);
                
                // Show final result
                setTimeout(() => {
                    dice1El.textContent = result1;
                    dice2El.textContent = result2;
                    dice1El.classList.remove('rolling');
                    dice2El.classList.remove('rolling');
                    
                    if (callback) callback();
                }, 300);
            }
        }, 100);
    }
    
    // Form validation
    static validatePlayerName(name) {
        if (!name || name.trim().length === 0) {
            return { valid: false, message: 'Il nome non può essere vuoto' };
        }
        
        if (name.trim().length < 2) {
            return { valid: false, message: 'Il nome deve essere di almeno 2 caratteri' };
        }
        
        if (name.trim().length > 20) {
            return { valid: false, message: 'Il nome non può superare i 20 caratteri' };
        }
        
        // Check for inappropriate content (basic check)
        const inappropriate = ['admin', 'bot', 'system', 'null', 'undefined'];
        if (inappropriate.some(word => name.toLowerCase().includes(word))) {
            return { valid: false, message: 'Nome non valido' };
        }
        
        return { valid: true };
    }
    
    static validateGameName(name) {
        if (!name || name.trim().length === 0) {
            return { valid: false, message: 'Il nome della partita non può essere vuoto' };
        }
        
        if (name.trim().length < 3) {
            return { valid: false, message: 'Il nome deve essere di almeno 3 caratteri' };
        }
        
        if (name.trim().length > 30) {
            return { valid: false, message: 'Il nome non può superare i 30 caratteri' };
        }
        
        return { valid: true };
    }
    
    // Player sorting helpers
    static sortPlayersByPosition(players) {
        return Object.values(players).sort((a, b) => a.position - b.position);
    }
    
    static sortPlayersByRoll(players) {
        return Object.values(players).sort((a, b) => (b.roll || 0) - (a.roll || 0));
    }
    
    static sortPlayersByName(players) {
        return Object.values(players).sort((a, b) => a.name.localeCompare(b.name));
    }
    
    // DOM helpers
    static createElement(tag, className = '', innerHTML = '') {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (innerHTML) element.innerHTML = innerHTML;
        return element;
    }
    
    static clearElement(element) {
        if (element) {
            element.innerHTML = '';
        }
    }
    
    // Button state helpers
    static enableButton(button, text = null) {
        if (!button) return;
        button.classList.remove('disabled');
        button.disabled = false;
        if (text) button.innerHTML = text;
    }
    
    static disableButton(button, text = null) {
        if (!button) return;
        button.classList.add('disabled');
        button.disabled = true;
        if (text) button.innerHTML = text;
    }
    
    // Network helpers
    static async makeRequest(url, options = {}) {
        try {
            const response = await fetch(url, {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Request failed:', error);
            throw error;
        }
    }
    
    // Firebase notification helper
    static async sendGameNotification(gameCode, senderUid, params = '') {
        try {
            const url = `https://shiftanddrift.onrender.com/check-and-notify?senderUid=${senderUid}&gameCode=${gameCode}${params}`;
            const response = await fetch(url);
            console.log('Notification sent:', response.status);
        } catch (error) {
            console.error('Failed to send notification:', error);
        }
    }
    
    // Debounce function for performance
    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
    
    // Copy to clipboard
    static async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            this.showToast('Copiato negli appunti!', 'success');
        } catch (error) {
            console.error('Failed to copy:', error);
            // Fallback for older browsers
            const textArea = document.createElement('textarea');
            textArea.value = text;
            document.body.appendChild(textArea);
            textArea.select();
            try {
                document.execCommand('copy');
                this.showToast('Copiato negli appunti!', 'success');
            } catch (err) {
                this.showToast('Impossibile copiare', 'error');
            }
            document.body.removeChild(textArea);
        }
    }
    
    // Device detection
    static isMobile() {
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    }
    
    static isIOS() {
        return /iPad|iPhone|iPod/.test(navigator.userAgent);
    }
    
    // Sound helpers (for future enhancement)
    static playSound(soundName) {
        // Placeholder for future sound implementation
        console.log(`Playing sound: ${soundName}`);
    }
    
    // Animation helpers
    static fadeIn(element, duration = 300) {
        if (!element) return;
        
        element.style.opacity = '0';
        element.style.display = 'block';
        
        let start = Date.now();
        const fade = () => {
            const elapsed = Date.now() - start;
            const progress = Math.min(elapsed / duration, 1);
            
            element.style.opacity = progress;
            
            if (progress < 1) {
                requestAnimationFrame(fade);
            }
        };
        
        requestAnimationFrame(fade);
    }
    
    static fadeOut(element, duration = 300, callback = null) {
        if (!element) return;
        
        let start = Date.now();
        const fade = () => {
            const elapsed = Date.now() - start;
            const progress = Math.min(elapsed / duration, 1);
            
            element.style.opacity = 1 - progress;
            
            if (progress < 1) {
                requestAnimationFrame(fade);
            } else {
                element.style.display = 'none';
                if (callback) callback();
            }
        };
        
        requestAnimationFrame(fade);
    }
}

// Export for use in other files
window.Utils = Utils;