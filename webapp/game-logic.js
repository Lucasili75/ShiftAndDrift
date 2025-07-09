/* ==============================================
   SHIFT & DRIFT - Game Logic & Firebase Management
   ============================================== */

class GameLogic {
    constructor() {
        this.db = firebase.database();
        this.messaging = firebase.messaging();
        this.uid = Utils.getUID();
        this.currentGame = null;
        this.gameListener = null;
        this.gamesListener = null;
        this.playerPrefs = Utils.getPlayerPrefs();
        
        // Initialize FCM
        this.initializeMessaging();
    }
    
    // =====================================
    // FIREBASE MESSAGING SETUP
    // =====================================
    
    async initializeMessaging() {
        try {
            // Register FCM token
            const token = await this.messaging.getToken({
                vapidKey: "BNDKTXEWE1-SPEZ-gc4dmgX7fRUae820aGiH9o_4WdA3KkRikUUwuMWQooC0odL4SCmmbnVLgT5rHnoICuPFO00"
            });
            
            if (token) {
                console.log('FCM Token:', token);
                // Save token to Firebase
                await this.db.ref(`tokens/${this.uid}`).set(token);
            }
            
            // Handle foreground messages
            this.messaging.onMessage(payload => {
                console.log('📬 Messaggio ricevuto:', payload);
                
                if (payload.notification) {
                    Utils.showToast(
                        `${payload.notification.title}: ${payload.notification.body}`,
                        'info',
                        6000
                    );
                }
                
                // Handle game state changes
                if (payload.data && payload.data.gameCode && this.currentGame) {
                    this.refreshGameData();
                }
            });
            
        } catch (error) {
            console.error('Error initializing messaging:', error);
        }
    }
    
    // =====================================
    // PLAYER MANAGEMENT
    // =====================================
    
    savePlayerPrefs(playerData) {
        this.playerPrefs = playerData;
        Utils.savePlayerPrefs(playerData);
    }
    
    getPlayerPrefs() {
        return this.playerPrefs;
    }
    
    // =====================================
    // GAME MANAGEMENT
    // =====================================
    
    async createGame(gameName, totalLaps = 2) {
        try {
            Utils.showLoading('Creazione partita...');
            
            const gameCode = Utils.generateGameCode();
            const gameData = {
                code: gameCode,
                name: gameName.trim(),
                status: 'waiting',
                track: '',
                currentTurn: 0,
                totalLaps: parseInt(totalLaps),
                host: this.uid,
                players: {},
                currentPlayerUid: null,
                createdAt: Date.now()
            };
            
            // Create game in Firebase
            await this.db.ref(`games/${gameCode}`).set(gameData);
            
            // Join the game
            await this.joinGame(gameData);
            
            Utils.hideLoading();
            Utils.showToast('Partita creata con successo!', 'success');
            
            return gameCode;
            
        } catch (error) {
            Utils.hideLoading();
            Utils.showToast('Errore nella creazione della partita', 'error');
            console.error('Error creating game:', error);
            throw error;
        }
    }
    
    async joinGame(game) {
        try {
            Utils.showLoading('Ingresso in partita...');
            
            // Check if player is already in game
            if (game.players && game.players[this.uid]) {
                Utils.hideLoading();
                this.navigateToGameScreen(game);
                return;
            }
            
            // Create player object
            const player = {
                uid: this.uid,
                name: this.playerPrefs.name,
                gear: 1,
                position: 0,
                carColorFront: this.playerPrefs.carColorFront,
                carColorRear: this.playerPrefs.carColorRear,
                carColorBody: this.playerPrefs.carColorBody,
                playerType: 'player',
                aggressiveness: 0,
                riskiness: 0,
                lap: 0,
                roll: -1,
                turn: 0,
                row: -1,
                column: -1,
                tires: 0,
                brakes: 0,
                body: 0,
                fuel: 0,
                engine: 0,
                curveStops: 0,
                status: ''
            };
            
            // Add player to game
            const updatedPlayers = { ...game.players, [this.uid]: player };
            await this.db.ref(`games/${game.code}/players`).set(updatedPlayers);
            
            // Send notification to other players
            if (Object.keys(game.players || {}).length > 0) {
                await Utils.sendGameNotification(
                    game.code,
                    this.uid,
                    `&fun=newPlayer&player=${this.playerPrefs.name}`
                );
            }
            
            Utils.hideLoading();
            Utils.showToast(`Ingresso nella partita "${game.name}"`, 'success');
            
            // Navigate to appropriate screen
            this.navigateToGameScreen(game);
            
        } catch (error) {
            Utils.hideLoading();
            Utils.showToast('Errore nell\'ingresso in partita', 'error');
            console.error('Error joining game:', error);
            throw error;
        }
    }
    
    async leaveGame() {
        if (!this.currentGame) return;
        
        try {
            Utils.showLoading('Uscita dalla partita...');
            
            // Remove player from game
            await this.db.ref(`games/${this.currentGame.code}/players/${this.uid}`).remove();
            
            // Send notification
            await Utils.sendGameNotification(
                this.currentGame.code,
                this.uid,
                `&fun=deletePlayer&player=${this.playerPrefs.name}`
            );
            
            // Clean up listeners
            this.removeGameListener();
            this.currentGame = null;
            
            Utils.hideLoading();
            Utils.showToast('Hai abbandonato la partita', 'info');
            
            // Navigate back to lobby
            Utils.showScreen('gameLobby');
            
        } catch (error) {
            Utils.hideLoading();
            Utils.showToast('Errore nell\'uscita dalla partita', 'error');
            console.error('Error leaving game:', error);
        }
    }
    
    navigateToGameScreen(game) {
        switch (game.status) {
            case 'waiting':
                Utils.showScreen('gameWaiting');
                break;
            case 'rolling':
                Utils.showScreen('gridRoll');
                break;
            case 'started':
                Utils.showScreen('activeGame');
                break;
            case 'finished':
                Utils.showScreen('gameWaiting'); // Show results
                break;
            default:
                Utils.showScreen('gameWaiting');
        }
    }
    
    // =====================================
    // GAME LISTENERS
    // =====================================
    
    startListeningToGames(callback) {
        this.removeGamesListener();
        
        this.gamesListener = this.db.ref('games').on('value', snapshot => {
            const games = [];
            
            if (snapshot.exists()) {
                snapshot.forEach(gameSnap => {
                    const game = gameSnap.val();
                    if (game && game.code) {
                        games.push(game);
                    }
                });
            }
            
            // Sort by creation time (newest first)
            games.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
            
            callback(games);
        });
    }
    
    removeGamesListener() {
        if (this.gamesListener) {
            this.db.ref('games').off('value', this.gamesListener);
            this.gamesListener = null;
        }
    }
    
    startListeningToGame(gameCode, callback) {
        this.removeGameListener();
        
        this.gameListener = this.db.ref(`games/${gameCode}`).on('value', snapshot => {
            if (snapshot.exists()) {
                this.currentGame = snapshot.val();
                callback(this.currentGame);
            } else {
                // Game was deleted
                Utils.showToast('La partita è stata eliminata', 'warning');
                Utils.showScreen('gameLobby');
                this.currentGame = null;
            }
        });
    }
    
    removeGameListener() {
        if (this.gameListener && this.currentGame) {
            this.db.ref(`games/${this.currentGame.code}`).off('value', this.gameListener);
            this.gameListener = null;
        }
    }
    
    async refreshGameData() {
        if (!this.currentGame) return;
        
        try {
            const snapshot = await this.db.ref(`games/${this.currentGame.code}`).once('value');
            if (snapshot.exists()) {
                this.currentGame = snapshot.val();
                return this.currentGame;
            }
        } catch (error) {
            console.error('Error refreshing game data:', error);
        }
    }
    
    // =====================================
    // GAME ACTIONS
    // =====================================
    
    async startGame() {
        if (!this.currentGame || this.currentGame.host !== this.uid) {
            Utils.showToast('Solo il creatore può iniziare la partita', 'warning');
            return;
        }
        
        try {
            Utils.showLoading('Avvio partita...');
            
            // Update game status to rolling (grid determination)
            await this.db.ref(`games/${this.currentGame.code}/status`).set('rolling');
            
            // Reset all player rolls
            const players = this.currentGame.players || {};
            const updates = {};
            
            Object.keys(players).forEach(uid => {
                updates[`games/${this.currentGame.code}/players/${uid}/roll`] = -1;
            });
            
            await this.db.ref().update(updates);
            
            // Send notification
            await Utils.sendGameNotification(this.currentGame.code, this.uid);
            
            Utils.hideLoading();
            Utils.showToast('Partita avviata! Determinazione griglia...', 'success');
            
        } catch (error) {
            Utils.hideLoading();
            Utils.showToast('Errore nell\'avvio della partita', 'error');
            console.error('Error starting game:', error);
        }
    }
    
    async rollDice() {
        if (!this.currentGame) return;
        
        const player = this.currentGame.players[this.uid];
        if (!player || player.roll >= 0) {
            Utils.showToast('Hai già tirato i dadi', 'warning');
            return;
        }
        
        try {
            // Generate dice roll
            const dice1 = Math.floor(Math.random() * 6) + 1;
            const dice2 = Math.floor(Math.random() * 6) + 1;
            const total = dice1 + dice2;
            
            // Update player's roll
            await this.db.ref(`games/${this.currentGame.code}/players/${this.uid}/roll`).set(total);
            
            Utils.showToast(`Hai tirato: ${dice1} + ${dice2} = ${total}`, 'success');
            
            // Check if all players have rolled
            this.checkAllPlayersRolled();
            
            return { dice1, dice2, total };
            
        } catch (error) {
            Utils.showToast('Errore nel tirare i dadi', 'error');
            console.error('Error rolling dice:', error);
        }
    }
    
    async checkAllPlayersRolled() {
        if (!this.currentGame) return;
        
        const players = Object.values(this.currentGame.players);
        const allRolled = players.every(player => player.roll >= 0);
        
        if (allRolled && this.currentGame.host === this.uid) {
            // Determine starting grid
            setTimeout(async () => {
                try {
                    const sortedPlayers = Utils.sortPlayersByRoll(this.currentGame.players);
                    const updates = {};
                    
                    sortedPlayers.forEach((player, index) => {
                        updates[`games/${this.currentGame.code}/players/${player.uid}/position`] = index + 1;
                    });
                    
                    // Start the race
                    updates[`games/${this.currentGame.code}/status`] = 'started';
                    updates[`games/${this.currentGame.code}/currentPlayerUid`] = sortedPlayers[0].uid;
                    
                    await this.db.ref().update(updates);
                    
                    // Send notification
                    await Utils.sendGameNotification(this.currentGame.code, this.uid);
                    
                } catch (error) {
                    console.error('Error setting up race:', error);
                }
            }, 2000);
        }
    }
    
    async makeMove(gear) {
        if (!this.currentGame || this.currentGame.currentPlayerUid !== this.uid) {
            Utils.showToast('Non è il tuo turno', 'warning');
            return;
        }
        
        try {
            Utils.showLoading('Esecuzione mossa...');
            
            const player = this.currentGame.players[this.uid];
            const movement = parseInt(gear);
            
            // Update player position (simplified logic)
            const newPosition = Math.max(0, player.position + movement);
            
            const updates = {};
            updates[`games/${this.currentGame.code}/players/${this.uid}/position`] = newPosition;
            updates[`games/${this.currentGame.code}/players/${this.uid}/gear`] = parseInt(gear);
            updates[`games/${this.currentGame.code}/currentTurn`] = (this.currentGame.currentTurn || 0) + 1;
            
            // Determine next player
            const players = Utils.sortPlayersByPosition(this.currentGame.players);
            const currentIndex = players.findIndex(p => p.uid === this.uid);
            const nextIndex = (currentIndex + 1) % players.length;
            updates[`games/${this.currentGame.code}/currentPlayerUid`] = players[nextIndex].uid;
            
            await this.db.ref().update(updates);
            
            // Send notification
            await Utils.sendGameNotification(this.currentGame.code, this.uid);
            
            Utils.hideLoading();
            Utils.showToast(`Movimento eseguito con marcia ${gear}`, 'success');
            
        } catch (error) {
            Utils.hideLoading();
            Utils.showToast('Errore nell\'esecuzione della mossa', 'error');
            console.error('Error making move:', error);
        }
    }
    
    // =====================================
    // GAME HELPERS
    // =====================================
    
    isPlayerInGame(game, uid = null) {
        const playerId = uid || this.uid;
        return game.players && game.players[playerId];
    }
    
    isGameHost(game, uid = null) {
        const playerId = uid || this.uid;
        return game.host === playerId;
    }
    
    canStartGame(game) {
        return this.isGameHost(game) && 
               game.status === 'waiting' && 
               Object.keys(game.players || {}).length >= 2;
    }
    
    getPlayerCount(game) {
        return Object.keys(game.players || {}).length;
    }
    
    getCurrentPlayer(game) {
        if (!game.currentPlayerUid || !game.players) return null;
        return game.players[game.currentPlayerUid];
    }
    
    isMyTurn(game) {
        return game.currentPlayerUid === this.uid;
    }
    
    getPlayersInOrder(game, sortBy = 'position') {
        if (!game.players) return [];
        
        switch (sortBy) {
            case 'position':
                return Utils.sortPlayersByPosition(game.players);
            case 'roll':
                return Utils.sortPlayersByRoll(game.players);
            case 'name':
                return Utils.sortPlayersByName(game.players);
            default:
                return Object.values(game.players);
        }
    }
    
    // =====================================
    // CLEANUP
    // =====================================
    
    cleanup() {
        this.removeGameListener();
        this.removeGamesListener();
        this.currentGame = null;
    }
    
    // =====================================
    // BOT MANAGEMENT (Future Enhancement)
    // =====================================
    
    async addBot(gameCode) {
        try {
            const botNames = [
                "Lewis Hammel", "Sebastien Vettle", "Mika Häknin", "Nico Rosbrek", "Fernando Alonze",
                "Jenson Butner", "Kimi Reikkon", "Max Verstappenko", "Daniel Rickardo", "Charles Leclaire"
            ];
            
            const randomName = botNames[Math.floor(Math.random() * botNames.length)];
            const botUid = 'bot_' + Math.random().toString(36).substr(2, 9);
            
            const bot = {
                uid: botUid,
                name: randomName,
                gear: 1,
                position: 0,
                carColorFront: 'Rosso',
                carColorRear: 'Rosso',
                carColorBody: 'Rosso',
                playerType: 'bot',
                aggressiveness: Math.floor(Math.random() * 10),
                riskiness: Math.floor(Math.random() * 10),
                lap: 0,
                roll: -1,
                turn: 0,
                row: -1,
                column: -1,
                tires: 0,
                brakes: 0,
                body: 0,
                fuel: 0,
                engine: 0,
                curveStops: 0,
                status: ''
            };
            
            await this.db.ref(`games/${gameCode}/players/${botUid}`).set(bot);
            
            Utils.showToast(`Bot ${randomName} aggiunto alla partita`, 'success');
            
        } catch (error) {
            Utils.showToast('Errore nell\'aggiunta del bot', 'error');
            console.error('Error adding bot:', error);
        }
    }
}

// Export for use in main app
window.GameLogic = GameLogic;