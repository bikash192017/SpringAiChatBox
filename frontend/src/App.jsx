import { useEffect, useRef, useState, useCallback } from "react";
import ReactMarkdown from "react-markdown";
import "./App.css";
import Login from "./Login";

function App() {
  // =====================================================
  // USER & AUTH STATE
  // =====================================================

  const [token, setToken] = useState(() => localStorage.getItem("token") || null);

  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem("user");
    return savedUser ? JSON.parse(savedUser) : null;
  });

  // =====================================================
  // CHAT STATE
  // =====================================================

  const [chatHistory, setChatHistory] = useState([]);
  const [conversationId, setConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  // =====================================================
  // REFS
  // =====================================================

  const socketRef = useRef(null);
  const messagesEndRef = useRef(null);
  const isCreatingConversation = useRef(false);

  // =====================================================
  // AUTO SCROLL
  // =====================================================

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // =====================================================
  // LOAD CHAT HISTORY
  // =====================================================

  const loadChatHistory = useCallback(async () => {
    if (!token) return;

    try {
      console.log("Loading chat history...");
      const response = await fetch(
        `${import.meta.env.VITE_API_URL}/api/chat/history`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        console.error("Failed to load chat history:", response.status);
        if (response.status === 401 || response.status === 403) {
          handleLogout();
        }
        return;
      }

      const conversations = await response.json();
      setChatHistory(conversations || []);
    } catch (error) {
      console.error("Error loading chat history:", error);
    }
  }, [token]);

  // =====================================================
  // CREATE NEW CONVERSATION
  // =====================================================

  const createConversation = useCallback(async () => {
    if (!token || isCreatingConversation.current) return null;
    isCreatingConversation.current = true;

    try {
      console.log("Creating new conversation...");
      const response = await fetch(
        `${import.meta.env.VITE_API_URL}/api/chat/conversation`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            title: "New Chat",
          }),
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        console.error("Create conversation failed:", response.status, errorText);
        return null;
      }

      const conversation = await response.json();
      setChatHistory((prev) => [conversation, ...prev]);
      setConversationId(conversation.id);
      setMessages([]);
      setInput("");
      setLoading(false);

      return conversation.id;
    } catch (error) {
      console.error("Error creating conversation:", error);
      return null;
    } finally {
      isCreatingConversation.current = false;
    }
  }, [token]);

  // =====================================================
  // INITIALIZE DATA UPON AUTH
  // =====================================================

  useEffect(() => {
    if (!user || !token) return;
    loadChatHistory();
  }, [user, token, loadChatHistory]);

  // Auto-select or create first chat
  useEffect(() => {
    if (!user || !token) return;
    if (chatHistory.length === 0 && !conversationId) {
      createConversation();
    }
  }, [user, token, chatHistory.length, conversationId, createConversation]);

  // =====================================================
  // SELECT CONVERSATION
  // =====================================================

  const selectConversation = (conversation) => {
    setConversationId(conversation.id);
    setMessages(conversation.messages || []);
    setInput("");
    setLoading(false);
  };

  // =====================================================
  // WEBSOCKET CONNECTION
  // =====================================================

  useEffect(() => {
    if (!user || !token) return;

    console.log("Creating WebSocket connection...");
    const wsUrl = `${import.meta.env.VITE_API_URL.replace(/^http/, "ws")}/ws/chat?token=${encodeURIComponent(token)}`;
    const socket = new WebSocket(wsUrl);

    socketRef.current = socket;

    socket.onopen = () => {
      console.log("WebSocket connected");
    };

    socket.onmessage = (event) => {
      const chunk = event.data;

      if (chunk === "[DONE]") {
        setLoading(false);
        loadChatHistory();
        return;
      }

      setMessages((previousMessages) => {
        const updatedMessages = [...previousMessages];
        const lastIndex = updatedMessages.length - 1;
        const lastMessage = updatedMessages[lastIndex];

        if (lastMessage && lastMessage.role === "assistant") {
          updatedMessages[lastIndex] = {
            ...lastMessage,
            content: lastMessage.content + chunk,
          };
        }
        return updatedMessages;
      });
    };

    socket.onerror = (error) => {
      console.error("WebSocket error:", error);
      setLoading(false);
    };

    socket.onclose = () => {
      console.log("WebSocket disconnected");
    };

    return () => {
      if (
        socket.readyState === WebSocket.OPEN ||
        socket.readyState === WebSocket.CONNECTING
      ) {
        socket.close();
      }
      if (socketRef.current === socket) {
        socketRef.current = null;
      }
    };
  }, [user, token, loadChatHistory]);

  // =====================================================
  // SEND MESSAGE
  // =====================================================

  const sendMessage = () => {
    const userMessage = input.trim();

    if (!userMessage || loading || !conversationId) return;

    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) {
      console.error("WebSocket is not connected");
      return;
    }

    setMessages((prev) => [
      ...prev,
      { role: "user", content: userMessage },
      { role: "assistant", content: "" },
    ]);

    setInput("");
    setLoading(true);

    const request = {
      conversationId: conversationId,
      message: userMessage,
    };

    socketRef.current.send(JSON.stringify(request));
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  // =====================================================
  // LOGOUT
  // =====================================================

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    if (socketRef.current) {
      socketRef.current.close();
    }

    setToken(null);
    setUser(null);
    setChatHistory([]);
    setConversationId(null);
    setMessages([]);
    setInput("");
    setLoading(false);
  };

  // =====================================================
  // AUTH (LOGIN & SIGN UP)
  // =====================================================

  if (!user || !token) {
    return (
      <Login
        onAuthSuccess={(data) => {
          localStorage.setItem("token", data.token);
          const userData = {
            id: data.id || data.userId,
            name: data.name,
            email: data.email,
          };
          localStorage.setItem("user", JSON.stringify(userData));

          // Update state in sync
          setToken(data.token);
          setUser(userData);
        }}
      />
    );
  }

  // =====================================================
  // MAIN UI
  // =====================================================

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="logo">
          <div className="logo-icon">✦</div>
          <span>Spring AI</span>
        </div>

        <button className="new-chat-btn" onClick={createConversation} disabled={loading}>
          <span>＋</span> New Chat
        </button>

        <div className="chat-history">
          <div className="history-title">Your Chats</div>
          {chatHistory.length === 0 ? (
            <div className="no-history">No previous chats</div>
          ) : (
            chatHistory.map((conversation) => (
              <button
                key={conversation.id}
                className={`chat-history-item ${
                  conversation.id === conversationId ? "active" : ""
                }`}
                onClick={() => selectConversation(conversation)}
              >
                <span className="chat-history-icon">💬</span>
                <span className="chat-history-title">
                  {conversation.title || "New Chat"}
                </span>
              </button>
            ))
          )}
        </div>

        <div className="sidebar-bottom">
          <div className="user-info">
            <div className="user-avatar">
              {user.name?.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="user-name">{user.name}</div>
              <div className="user-email">{user.email}</div>
            </div>
          </div>

          <button className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>

      <main className="chat-container">
        <header className="chat-header">
          <div>
            <h1>
              {chatHistory.find((chat) => chat.id === conversationId)?.title ||
                "AI Assistant"}
            </h1>
            <p>Powered by Spring AI</p>
          </div>
          <div className="connection-status">
            <span className="status-dot"></span> Connected
          </div>
        </header>

        <section className="messages">
          {messages.length === 0 ? (
            <div className="welcome">
              <div className="welcome-icon">✦</div>
              <h2>How can I help you?</h2>
              <p>Ask me anything and I'll try my best to help you.</p>

              <div className="suggestions">
                <button
                  onClick={() =>
                    setInput("Explain dependency injection in Spring Boot")
                  }
                >
                  Explain Dependency Injection
                </button>
                <button onClick={() => setInput("What is Spring AI?")}>
                  What is Spring AI?
                </button>
                <button
                  onClick={() =>
                    setInput("Explain microservices in simple terms")
                  }
                >
                  Explain Microservices
                </button>
              </div>
            </div>
          ) : (
            messages.map((message, index) => (
              <div key={index} className={`message-row ${message.role}`}>
                <div className="avatar">
                  {message.role === "user" ? "You" : "AI"}
                </div>
                <div className="message-content">
                  <div className="message-role">
                    {message.role === "user" ? "You" : "AI Assistant"}
                  </div>
                  <div className="message-text">
                    <ReactMarkdown>{message.content}</ReactMarkdown>
                    {message.role === "assistant" &&
                      loading &&
                      index === messages.length - 1 && (
                        <span className="cursor">▌</span>
                      )}
                  </div>
                </div>
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </section>

        <div className="input-area">
          <div className="input-wrapper">
            <textarea
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={
                conversationId
                  ? "Message AI Assistant..."
                  : "Creating new chat..."
              }
              rows="1"
              disabled={loading || !conversationId}
            />
            <button
              className="send-btn"
              onClick={sendMessage}
              disabled={!input.trim() || loading || !conversationId}
            >
              ↑
            </button>
          </div>
          <p className="input-hint">
            Press Enter to send · Shift + Enter for new line
          </p>
        </div>
      </main>
    </div>
  );
}

export default App;