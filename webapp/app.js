/* ==============================================
   SHIFT & DRIFT - Main Application
   ============================================== */

class ShiftDriftApp {
    constructor() {
        this.gameLogic = new GameLogic();
        this.currentScreen = 'mainMenu';
        
        this.initializeApp();
    }
    
    async initializeApp() {
        try {
            // Load player preferences
            const playerPrefs = this.gameLogic.getPlayerPrefs();
            this.updatePlayerDisplay(playerPrefs);
            
            // Initialize event listeners
            this.initializeEventListeners();
            
            // Show main menu
            Utils.showScreen('mainMenu');
            
            console.log('✅ Shift & Drift initialized successfully');
            
        } catch (error) {
            console.error('❌ Error initializing app:', error);
            Utils.showToast('Errore nell\'inizializzazione dell\'app', 'error');
        }
    }
    
    // =====================================
    // EVENT LISTENERS SETUP
    // =====================================
    
    initializeEventListeners() {
        // Main Menu Events
        document.getElementById('playBtn')?.addEventListener('click', () => {
            this.showGameLobby();
        });
        
        document.getElementById('changeNameBtn')?.addEventListener('click', () => {
            this.showPlayerNameScreen();
        });
        
        // Player Name Form Events
        document.getElementById('playerForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.savePlayerProfile();
        });
        
        document.getElementById('cancelPlayerBtn')?.addEventListener('click', () => {
            Utils.showScreen('mainMenu');
        });
        
        // Car color change events
        ['frontColorSelect', 'bodyColorSelect', 'rearColorSelect'].forEach(id => {
            document.getElementById(id)?.addEventListener('change', () => {
                this.updateCarPreview();
            });
        });
        
        // Game Lobby Events
        document.getElementById('createGameBtn')?.addEventListener('click', () => {
            Utils.showModal('createGameModal');
        });
        
        document.getElementById('refreshGamesBtn')?.addEventListener('click', () => {
            this.refreshGamesList();
        });
        
        // Create Game Modal Events
        document.getElementById('createGameForm')?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.createNewGame();
        });
        
        // Modal close events
        document.querySelectorAll('.close-modal').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const modal = e.target.closest('.modal');
                if (modal) {
                    Utils.hideModal(modal.id);
                }
            });
        });
        
        // Game Waiting Room Events
        document.getElementById('startGameBtn')?.addEventListener('click', () => {
            this.gameLogic.startGame();
        });
        
        document.getElementById('leaveGameBtn')?.addEventListener('click', () => {
            this.gameLogic.leaveGame();
        });
        
        // Grid Roll Events
        document.getElementById('rollDiceBtn')?.addEventListener('click', () => {
            this.handleDiceRoll();
        });
        
        // Active Game Events
        document.getElementById('moveBtn')?.addEventListener('click', () => {
            this.handleMove();
        });
        
        // Game code click to copy
        document.getElementById('gameCode')?.addEventListener('click', (e) => {
            const code = e.target.textContent.replace('Codice: ', '');
            Utils.copyToClipboard(code);
        });
        
        // Modal backdrop clicks
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    Utils.hideModal(modal.id);
                }
            });
        });
    }
    
    // =====================================
    // PLAYER PROFILE MANAGEMENT
    // =====================================
    
    showPlayerNameScreen() {
        const playerPrefs = this.gameLogic.getPlayerPrefs();
        
        // Populate form with current values
        document.getElementById('playerNameInput').value = playerPrefs.name;
        document.getElementById('frontColorSelect').value = playerPrefs.carColorFront;
        document.getElementById('bodyColorSelect').value = playerPrefs.carColorBody;
        document.getElementById('rearColorSelect').value = playerPrefs.carColorRear;
        
        this.updateCarPreview();
        Utils.showScreen('playerNameScreen');
    }
    
    savePlayerProfile() {
        const name = document.getElementById('playerNameInput').value.trim();
        const frontColor = document.getElementById('frontColorSelect').value;
        const bodyColor = document.getElementById('bodyColorSelect').value;
        const rearColor = document.getElementById('rearColorSelect').value;
        
        // Validate input
        const validation = Utils.validatePlayerName(name);
        if (!validation.valid) {
            Utils.showToast(validation.message, 'error');
            return;
        }
        
        // Save preferences
        const playerData = {
            name: name,
            carColorFront: frontColor,
            carColorBody: bodyColor,
            carColorRear: rearColor
        };
        
        this.gameLogic.savePlayerPrefs(playerData);
        this.updatePlayerDisplay(playerData);
        
        Utils.showToast('Profilo salvato con successo!', 'success');
        Utils.showScreen('mainMenu');
    }
    
    updatePlayerDisplay(playerData) {
        // Update main menu display
        document.getElementById('mainPlayerName').textContent = `Giocatore: ${playerData.name}`;
        document.getElementById('playerNameDisplay').textContent = playerData.name;
        
        // Update car colors
        Utils.updateCarColors(playerData.carColorFront, playerData.carColorBody, playerData.carColorRear);
    }
    
    updateCarPreview() {
        const frontColor = document.getElementById('frontColorSelect').value;
        const bodyColor = document.getElementById('bodyColorSelect').value;
        const rearColor = document.getElementById('rearColorSelect').value;
        
        Utils.updateCarColors(frontColor, bodyColor, rearColor);
    }
    
    // =====================================
    // GAME LOBBY MANAGEMENT
    // =====================================
    
    showGameLobby() {
        Utils.showScreen('gameLobby');
        this.refreshGamesList();
        
        // Start listening to games
        this.gameLogic.startListeningToGames(games => {
            this.updateGamesList(games);
        });
    }
    
    refreshGamesList() {
        const gamesList = document.getElementById('gamesList');
        gamesList.innerHTML = '<div class="loading-state"><i class="fas fa-spinner fa-spin"></i><p>Caricamento partite...</p></div>';
    }
    
    updateGamesList(games) {
        const gamesList = document.getElementById('gamesList');
        Utils.clearElement(gamesList);
        
        if (games.length === 0) {
            gamesList.innerHTML = `
                <div class="loading-state">
                    <i class="fas fa-racing-flag"></i>
                    <p>Nessuna partita disponibile</p>
                    <small>Crea una nuova partita per iniziare!</small>
                </div>
            `;
            return;
        }
        
        games.forEach(game => {
            const gameItem = this.createGameItem(game);
            gamesList.appendChild(gameItem);
        });
    }
    
    createGameItem(game) {
        const gameItem = Utils.createElement('div', 'game-item');
        const playerCount = this.gameLogic.getPlayerCount(game);
        const isPlayerInGame = this.gameLogic.isPlayerInGame(game);
        
        gameItem.innerHTML = `
            <div class="game-info">
                <h3>${game.name}</h3>
                <div class="game-meta">
                    <span><i class="fas fa-users"></i> ${playerCount} giocatori</span>
                    <span><i class="fas fa-clock"></i> ${Utils.formatTime(game.createdAt)}</span>
                    <span><i class="fas fa-code"></i> ${game.code}</span>
                </div>
            </div>
            <div class="game-status">
                <span class="status-badge status-${game.status}">
                    ${Utils.getStatusText(game.status)}
                </span>
                ${isPlayerInGame ? '<i class="fas fa-check text-success" title="Sei in questa partita"></i>' : ''}
            </div>
        `;
        
        gameItem.addEventListener('click', () => {
            this.joinGame(game);
        });
        
        return gameItem;
    }
    
    async joinGame(game) {
        try {
            await this.gameLogic.joinGame(game);
            this.startGameListener(game.code);
        } catch (error) {
            console.error('Error joining game:', error);
        }
    }
    
    // =====================================
    // GAME CREATION
    // =====================================
    
    async createNewGame() {
        const gameName = document.getElementById('gameNameInput').value.trim();
        const totalLaps = document.getElementById('totalLapsInput').value;
        
        // Validate input
        const validation = Utils.validateGameName(gameName);
        if (!validation.valid) {
            Utils.showToast(validation.message, 'error');
            return;
        }
        
        try {
            const gameCode = await this.gameLogic.createGame(gameName, totalLaps);
            Utils.hideModal('createGameModal');
            
            // Reset form
            document.getElementById('createGameForm').reset();
            
            // Start listening to the new game
            this.startGameListener(gameCode);
            
        } catch (error) {
            console.error('Error creating game:', error);
        }
    }
    
    // =====================================
    // GAME STATE MANAGEMENT
    // =====================================
    
    startGameListener(gameCode) {
        this.gameLogic.startListeningToGame(gameCode, game => {
            this.updateGameScreen(game);
        });
    }
    
    updateGameScreen(game) {
        switch (game.status) {
            case 'waiting':
                this.updateWaitingRoom(game);
                break;
            case 'rolling':
                this.updateGridRollScreen(game);
                break;
            case 'started':
                this.updateActiveGameScreen(game);
                break;
            case 'finished':
                this.updateGameResults(game);
                break;
        }
    }
    
    // =====================================
    // WAITING ROOM SCREEN
    // =====================================
    
    updateWaitingRoom(game) {
        if (document.querySelector('.screen.active').id !== 'gameWaiting') {
            Utils.showScreen('gameWaiting');
        }
        
        // Update game info
        document.getElementById('gameTitle').textContent = game.name;
        document.getElementById('gameCode').textContent = `Codice: ${game.code}`;
        document.getElementById('gameStatus').textContent = Utils.getStatusText(game.status);
        document.getElementById('gameStatus').className = `status-badge status-${game.status}`;
        
        // Update player count
        const playerCount = this.gameLogic.getPlayerCount(game);
        document.getElementById('playerCount').textContent = `(${playerCount})`;
        
        // Update players list
        this.updatePlayersList(game);
        
        // Update start button visibility
        const startBtn = document.getElementById('startGameBtn');
        if (this.gameLogic.canStartGame(game)) {
            startBtn.classList.remove('hidden');
        } else {
            startBtn.classList.add('hidden');
        }
    }
    
    updatePlayersList(game) {
        const playersList = document.getElementById('playersList');
        Utils.clearElement(playersList);
        
        const players = this.gameLogic.getPlayersInOrder(game, 'name');
        
        players.forEach(player => {
            const playerItem = this.createPlayerItem(player, game);
            playersList.appendChild(playerItem);
        });
    }
    
    createPlayerItem(player, game) {
        const playerItem = Utils.createElement('div', 'player-item');
        const isHost = this.gameLogic.isGameHost(game, player.uid);
        const isMe = player.uid === this.gameLogic.uid;
        
        playerItem.innerHTML = `
            <div class="player-avatar">
                ${player.name.charAt(0).toUpperCase()}
            </div>
            <div class="player-details">
                <h4>
                    ${player.name}
                    ${isHost ? '<i class="fas fa-crown text-warning" title="Creatore"></i>' : ''}
                    ${isMe ? '<i class="fas fa-user text-primary" title="Tu"></i>' : ''}
                </h4>
                <div class="player-car-colors">
                    <span class="color-dot" style="background-color: ${Utils.getCarColorHex(player.carColorFront)}"></span>
                    <span class="color-dot" style="background-color: ${Utils.getCarColorHex(player.carColorBody)}"></span>
                    <span class="color-dot" style="background-color: ${Utils.getCarColorHex(player.carColorRear)}"></span>
                </div>
            </div>
        `;
        
        return playerItem;
    }
    
    // =====================================
    // GRID ROLL SCREEN
    // =====================================
    
    updateGridRollScreen(game) {
        if (document.querySelector('.screen.active').id !== 'gridRoll') {
            Utils.showScreen('gridRoll');
        }
        
        // Update turn info
        this.updateRollTurnInfo(game);
        
        // Update dice button state
        this.updateDiceButton(game);
        
        // Update standings
        this.updateGridStandings(game);
    }
    
    updateRollTurnInfo(game) {
        const turnInfo = document.getElementById('turnInfo');
        const player = game.players[this.gameLogic.uid];
        
        if (player && player.roll >= 0) {
            turnInfo.textContent = `Hai tirato: ${player.roll}. Aspetta gli altri giocatori...`;
        } else {
            turnInfo.textContent = `È il tuo turno! Tira i dadi per determinare la posizione di partenza.`;
        }
    }
    
    updateDiceButton(game) {
        const rollBtn = document.getElementById('rollDiceBtn');
        const player = game.players[this.gameLogic.uid];
        
        if (player && player.roll >= 0) {
            Utils.disableButton(rollBtn, '<i class="fas fa-check"></i> Già tirato');
        } else {
            Utils.enableButton(rollBtn, '<i class="fas fa-dice"></i> Tira i Dadi');
        }
    }
    
    updateGridStandings(game) {
        const standings = document.getElementById('gridStandings');
        Utils.clearElement(standings);
        
        const players = this.gameLogic.getPlayersInOrder(game, 'roll');
        
        players.forEach((player, index) => {
            if (player.roll >= 0) {
                const standingItem = Utils.createElement('div', 'grid-item');
                standingItem.innerHTML = `
                    <div class="grid-position">${index + 1}°</div>
                    <div class="player-name">${player.name}</div>
                    <div class="player-roll">Tiro: ${player.roll}</div>
                `;
                standings.appendChild(standingItem);
            }
        });
    }
    
    async handleDiceRoll() {
        try {
            const result = await this.gameLogic.rollDice();
            if (result) {
                // Animate dice
                const dice1 = document.getElementById('dice1');
                const dice2 = document.getElementById('dice2');
                
                Utils.rollDice(dice1, dice2, result.dice1, result.dice2, () => {
                    // Animation complete
                });
            }
        } catch (error) {
            console.error('Error rolling dice:', error);
        }
    }
    
    // =====================================
    // ACTIVE GAME SCREEN
    // =====================================
    
    updateActiveGameScreen(game) {
        if (document.querySelector('.screen.active').id !== 'activeGame') {
            Utils.showScreen('activeGame');
        }
        
        // Update game header
        document.getElementById('activeGameTitle').textContent = game.name;
        document.getElementById('currentLap').textContent = '1'; // Simplified
        document.getElementById('totalLaps').textContent = game.totalLaps || 2;
        document.getElementById('currentTurn').textContent = game.currentTurn || 1;
        
        // Update current player info
        this.updateCurrentPlayerInfo(game);
        
        // Update move controls
        this.updateMoveControls(game);
        
        // Update race standings
        this.updateRaceStandings(game);
        
        // Update track (simplified)
        this.updateTrack(game);
    }
    
    updateCurrentPlayerInfo(game) {
        const currentPlayer = this.gameLogic.getCurrentPlayer(game);
        const playerInfo = document.getElementById('currentPlayerInfo');
        
        if (currentPlayer) {
            if (this.gameLogic.isMyTurn(game)) {
                playerInfo.textContent = 'È il tuo turno!';
                playerInfo.className = 'current-player text-primary';
            } else {
                playerInfo.textContent = `È il turno di ${currentPlayer.name}`;
                playerInfo.className = 'current-player';
            }
        }
    }
    
    updateMoveControls(game) {
        const moveBtn = document.getElementById('moveBtn');
        
        if (this.gameLogic.isMyTurn(game)) {
            Utils.enableButton(moveBtn, '<i class="fas fa-forward"></i> Muovi');
        } else {
            Utils.disableButton(moveBtn, '<i class="fas fa-clock"></i> Aspetta');
        }
    }
    
    updateRaceStandings(game) {
        const standings = document.getElementById('raceStandings');
        Utils.clearElement(standings);
        
        const players = this.gameLogic.getPlayersInOrder(game, 'position');
        
        players.forEach((player, index) => {
            const standingItem = Utils.createElement('div', 'standing-item');
            standingItem.innerHTML = `
                <div class="position">${index + 1}°</div>
                <div class="name">${player.name}</div>
                <div class="pos">${player.position}</div>
            `;
            standings.appendChild(standingItem);
        });
    }
    
    updateTrack(game) {
        const track = document.getElementById('raceTrack');
        Utils.clearElement(track);
        
        // Simplified track visualization
        for (let i = 0; i < 6; i++) {
            const lane = Utils.createElement('div', 'track-lane');
            track.appendChild(lane);
        }
        
        // Place players on track
        const players = Object.values(game.players);
        players.forEach(player => {
            const playerDot = Utils.createElement('div', 'track-position');
            playerDot.style.backgroundColor = Utils.getCarColorHex(player.carColorBody);
            playerDot.style.left = `${Math.min(player.position * 20, 90)}%`;
            playerDot.textContent = player.name.charAt(0);
            
            const lane = track.children[Math.floor(Math.random() * 6)];
            lane.appendChild(playerDot);
        });
    }
    
    async handleMove() {
        const gear = document.getElementById('gearSelect').value;
        await this.gameLogic.makeMove(gear);
    }
    
    // =====================================
    // CLEANUP
    // =====================================
    
    cleanup() {
        this.gameLogic.cleanup();
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.shiftDriftApp = new ShiftDriftApp();
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (window.shiftDriftApp) {
        window.shiftDriftApp.cleanup();
    }
});
