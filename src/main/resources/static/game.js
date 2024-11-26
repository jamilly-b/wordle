const playerBoard = document.getElementById("player-board");
const opponentBoard = document.getElementById("opponent-board");
const guessInput = document.getElementById("guess-input");
const sendGuessButton = document.getElementById("send-guess");
const connectButton = document.getElementById("connect");
const statusDiv = document.getElementById("status");

let socket;
let isConnected = false;

function createBoard(board) {
    board.innerHTML = "";
    for (let i = 0; i < 30; i++) { // 6 linhas x 5 colunas
        const cell = document.createElement("div");
        cell.className = "cell";
        board.appendChild(cell);
    }
}

function updateBoard(board, result, rowIndex) {
    const start = rowIndex * 5;
    const cells = board.querySelectorAll(".cell");
    result.forEach((res, index) => {
        const cell = cells[start + index];
        cell.textContent = res.letter.toUpperCase();
        cell.className = `cell ${res.status}`;
    });
}

connectButton.addEventListener("click", () => {
    if (isConnected) {
        socket.close();
        isConnected = false;
        connectButton.textContent = "Conectar";
        statusDiv.textContent = "Desconectado.";
        return;
    }

    socket = new WebSocket("ws://localhost:8080/game");

    socket.onopen = () => {
        isConnected = true;
        connectButton.textContent = "Sair";
        statusDiv.textContent = "Aguardando outro jogador...";
    };

    socket.onmessage = (event) => {
        const data = JSON.parse(event.data);

        if (data.status === "start") {
            statusDiv.textContent = "Jogo iniciado! Envie sua palavra.";
            createBoard(playerBoard);
            createBoard(opponentBoard);
        } else if (data.status === "update") {
            updateBoard(playerBoard, data.result, data.rowIndex);
        } else if (data.status === "win" || data.status === "lose") {
            statusDiv.textContent = data.message;
            isConnected = false;
            connectButton.textContent = "Conectar";
        } else if (data.status === "opponent_left") {
            statusDiv.textContent = "Seu oponente desistiu! Você venceu!";
            isConnected = false;
            connectButton.textContent = "Conectar";
        }
    };

    socket.onclose = () => {
        statusDiv.textContent = "Conexão encerrada.";
        isConnected = false;
        connectButton.textContent = "Conectar";
    };
});

sendGuessButton.addEventListener("click", () => {
    if (!isConnected) {
        alert("Conecte-se ao servidor primeiro!");
        return;
    }

    const guessedWord = guessInput.value.trim().toLowerCase();
    if (guessedWord.length !== 5) {
        alert("A palavra deve ter 5 letras!");
        return;
    }

    const message = {
        type: "guess",
        word: guessedWord
    };
    socket.send(JSON.stringify(message));
    guessInput.value = "";
});
