'use strict';

const usernamePage = document.querySelector('#username-page');
const chatPage = document.querySelector('#chat-page');
const usernameForm = document.querySelector('#usernameForm');
const messageForm = document.querySelector('#messageForm');
const messageInput = document.querySelector('#message');
const connectingElement = document.querySelector('.connecting');
const chatArea = document.querySelector('#chat-messages');
const logout = document.querySelector('#logout');

let stompClient = null;
let nickname = null;
let fullname = null;
let selectedUserId = null;
let onlineUsers = new Set(); // Keep track of online users


function connect(event) {
    nickname = document.querySelector('#nickname').value.trim();
    fullname = document.querySelector('#fullname').value.trim();

    if (nickname && fullname) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    stompClient.subscribe(`/user/${nickname}/queue/messages`, onMessageReceived);
    stompClient.subscribe(`/user/${nickname}/queue/typing`, onMessageReceived);
    stompClient.subscribe(`/user/public`, onMessageReceived);
    stompClient.subscribe(`/topic/onlineUsers`, onActiveUsers);

    stompClient.send("/app/onlineUser",
                {},
                JSON.stringify({nickName: nickname, fullName: fullname})
            );


    console.log("Sent message to '/app/typingStatus' ");

     //register the connected user
    stompClient.send("/app/user/addUser",
        {},
        JSON.stringify({nickName: nickname, fullName: fullname, status: 'ONLINE'})
    );

    document.querySelector('#connected-user-fullname').textContent = fullname;
    findAndDisplayConnectedUsers().then();
}

function sendTyping() {
    if (stompClient && selectedUserId) {
        const typingMessage = {
            sender: nickname,
            recipientId: selectedUserId,
            typing: true
        };

        stompClient.send("/app/typingStatus", {}, JSON.stringify(typingMessage));

        clearTimeout(typingIndicator);
        typingIndicator = setTimeout(() => {
            const typingDiv = document.getElementById("typing");
            if (typingDiv) {
                typingDiv.textContent = "";
            }
        }, 1000);
    }
}

async function onActiveUsers(payload) {
    console.log("Connected to active users websocket");
    console.log("Payload from onActiveUsers:", payload.body);

    onlineUsers = new Set(JSON.parse(payload.body));

    await findAndDisplayConnectedUsers(); // Ensure users are in the DOM first
    updateUserStatus();
}
async function findAndDisplayConnectedUsers() {
    const connectedUsersResponse = await fetch('/users');
    let connectedUsers = await connectedUsersResponse.json();
    connectedUsers = connectedUsers.filter(user => user.nickName !== nickname);
    const connectedUsersList = document.getElementById('connectedUsers');
    connectedUsersList.innerHTML = '';

    connectedUsers.forEach(user => {
        appendUserElement(user, connectedUsersList);
        if (connectedUsers.indexOf(user) < connectedUsers.length - 1) {
            const separator = document.createElement('li');
            separator.classList.add('separator');
            connectedUsersList.appendChild(separator);
        }
    });

    updateUserStatus();
}

function appendUserElement(user, connectedUsersList) {
    const listItem = document.createElement('li');
    listItem.classList.add('user-item');
    listItem.id = user.nickName;

    const userImage = document.createElement('img');
    userImage.src = '../img/user_icon.png';
    userImage.alt = user.fullName;

    const usernameSpan = document.createElement('span');
    usernameSpan.textContent = user.fullName;

    const statusIndicator = document.createElement('span');
    statusIndicator.classList.add('status-indicator'); // Add the status dot

    listItem.appendChild(userImage);
    listItem.appendChild(usernameSpan);
    listItem.appendChild(statusIndicator);

    listItem.addEventListener('click', userItemClick);

    connectedUsersList.appendChild(listItem);
}


function updateUserStatus() {
    const userElements = document.querySelectorAll('.user-item');
    userElements.forEach(userElement => {
        const userId = userElement.id;
        let statusIndicator = userElement.querySelector('.status-indicator');

        if (!statusIndicator) {
            statusIndicator = document.createElement('span');
            statusIndicator.classList.add('status-indicator');
            userElement.appendChild(statusIndicator);
        }

        if (onlineUsers.has(userId)) {
            statusIndicator.classList.add('online');
            statusIndicator.classList.remove('offline');
        } else {
            statusIndicator.classList.add('offline');
            statusIndicator.classList.remove('online');
        }
    });
}


function userItemClick(event) {
    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
    });
    messageForm.classList.remove('hidden');

    const clickedUser = event.currentTarget;
    clickedUser.classList.add('active');

    selectedUserId = clickedUser.getAttribute('id');
    fetchAndDisplayUserChat().then();

    const nbrMsg = clickedUser.querySelector('.nbr-msg');
    nbrMsg.classList.add('hidden');
    nbrMsg.textContent = '0';

}

function displayMessage(senderId, content) {
    const messageContainer = document.createElement('div');
    messageContainer.classList.add('message');
    if (senderId === nickname) {
        messageContainer.classList.add('sender');
    } else {
        messageContainer.classList.add('receiver');
    }
    const message = document.createElement('p');
    message.textContent = content;
    messageContainer.appendChild(message);
    chatArea.appendChild(messageContainer);
}

async function fetchAndDisplayUserChat() {
    const userChatResponse = await fetch(`/messages/${nickname}/${selectedUserId}`);
    const userChat = await userChatResponse.json();
    chatArea.innerHTML = '';
    userChat.forEach(chat => {
        displayMessage(chat.senderId, chat.content);
    });
    chatArea.scrollTop = chatArea.scrollHeight;
}

function onError() {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}

function sendMessage(event) {
    const messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        const chatMessage = {
            senderId: nickname,
            recipientId: selectedUserId,
            content: messageInput.value.trim(),
            timestamp: new Date()
        };
        stompClient.send("/app/chat", {}, JSON.stringify(chatMessage));
        displayMessage(nickname, messageInput.value.trim());
        messageInput.value = '';
    }
    chatArea.scrollTop = chatArea.scrollHeight;
    event.preventDefault();
}

async function onMessageReceived(payload) {
    await findAndDisplayConnectedUsers();
    console.log('Message received', payload);
    const message = JSON.parse(payload.body);

    // Handle typing notifications
    if (message.typing && message.sender !== nickname) {
        // Display "typing..." in the user list
        const userListItem = document.getElementById(message.sender);
        if (userListItem) {
            const typingIndicator = userListItem.querySelector('.typing-indicator');
            if (!typingIndicator) {
                const newTypingIndicator = document.createElement('span');
                newTypingIndicator.classList.add('typing-indicator');
                newTypingIndicator.textContent = " is typing...";
                userListItem.appendChild(newTypingIndicator);
            } else {
                typingIndicator.textContent = " is typing...";
            }

            clearTimeout(userListItem.typingTimeout);
            userListItem.typingTimeout = setTimeout(() => {
                const typingIndicator = userListItem.querySelector('.typing-indicator');
                if (typingIndicator) {
                    typingIndicator.remove();
                }
            }, 1000);
        }

        // Display "typing..." in the main chat area (if selected)
        if (selectedUserId === message.sender) {
            const typingDiv = document.getElementById("typing");
            if (typingDiv) {
                typingDiv.textContent = message.sender + " is typing...";
                clearTimeout(typingIndicator);
                typingIndicator = setTimeout(() => {
                    typingDiv.textContent = "";
                }, 1000);
            }
        }
    }

    // Handle chat messages (non-typing)
    if (!message.typing) {
        if (selectedUserId && selectedUserId === message.senderId) {
            // Display message in chat area
            displayMessage(message.senderId, message.content);
            chatArea.scrollTop = chatArea.scrollHeight;
        } else {
            // Display notification dot if not in the same chat
            const notifiedUser = document.querySelector(`#${message.senderId}`);
            if (notifiedUser && !notifiedUser.classList.contains('active')) {
                const nbrMsg = notifiedUser.querySelector('.nbr-msg');
                if (!nbrMsg) {
                    const newNbrMsg = document.createElement('span');
                    newNbrMsg.classList.add('nbr-msg');
                    newNbrMsg.textContent = '';
                    notifiedUser.appendChild(newNbrMsg);
                } else {
                    nbrMsg.classList.remove('hidden');
                    nbrMsg.textContent = '';
                }
            }
        }
    }

    // Update active user status
    if (selectedUserId) {
        document.querySelector(`#${selectedUserId}`).classList.add('active');
    } else {
        messageForm.classList.add('hidden');
    }
}


function onLogout() {
    stompClient.send("/app/user/disconnectUser",
        {},
        JSON.stringify({nickName: nickname, fullName: fullname, status: 'OFFLINE'})
    );

    stompClient.send("/app/offlineUser",
            {},
            JSON.stringify({nickName: nickname, fullName: fullname, status: 'OFFLINE'})
        );
    window.location.reload();
}



usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
logout.addEventListener('click', onLogout, true);
//Add the typing div to the chat area.
chatArea.insertAdjacentHTML('afterend', '<div id="typing"></div>');

let typingIndicator;

//Add the oninput event to the message input.
messageInput.addEventListener('input', sendTyping);
window.onbeforeunload = () => onLogout();
