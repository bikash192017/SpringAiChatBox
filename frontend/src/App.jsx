import { useEffect, useRef, useState } from "react";
import ReactMarkdown from "react-markdown";
import "./App.css";
import Login from "./Login";

function App() {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem("user");

    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const socketRef = useRef(null);
  const messagesEndRef = useRef(null);

  // Automatically scroll to latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages]);

  // Connect WebSocket only after login
  useEffect(() => {
    if (!user) {
      return;
    }

    const token = localStorage.getItem("token");

    const socket = new WebSocket(
        `ws://localhost:8080/ws/chat?token=${encodeURIComponent(token)}`
    );

    socketRef.current = socket;

    socket.onopen = () => {
      console.log("WebSocket connected");
    };

    socket.onmessage = (event) => {
      const chunk = event.data;

      console.log("Received chunk:", chunk);

      if (chunk === "[DONE]") {
        console.log("AI response completed");
        setLoading(false);
        return;
      }

      setMessages((prev) => {
        const updatedMessages = [...prev];

        const lastMessage =
          updatedMessages[updatedMessages.length - 1];

        if (lastMessage?.role === "assistant") {
          updatedMessages[updatedMessages.length - 1] = {
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

      socketRef.current = null;
    };
  }, [user]);

  const sendMessage = () => {
    if (!input.trim() || loading) {
      return;
    }

    const userMessage = input.trim();

    if (
      !socketRef.current ||
      socketRef.current.readyState !== WebSocket.OPEN
    ) {
      console.error("WebSocket is not connected");
      return;
    }

    setMessages((prev) => [
      ...prev,
      {
        role: "user",
        content: userMessage,
      },
      {
        role: "assistant",
        content: "",
      },
    ]);

    setInput("");
    setLoading(true);

    socketRef.current.send(userMessage);
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  const startNewChat = () => {
    setMessages([]);
    setInput("");
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    if (socketRef.current) {
      socketRef.current.close();
    }

    setUser(null);
    setMessages([]);
    setInput("");
  };

  // Show Login page when user is not authenticated
  if (!user) {
    return (
      <Login
        onLogin={(data) => {
          setUser({
            id: data.id,
            name: data.name,
            email: data.email,
          });
        }}
      />
    );
  }

  return (
    <div className="app">

      {/* ================= SIDEBAR ================= */}

      <aside className="sidebar">

        <div className="logo">
          <div className="logo-icon">✦</div>
          <span>Spring AI</span>
        </div>

        <button
          className="new-chat-btn"
          onClick={startNewChat}
        >
          <span>＋</span>
          New Chat
        </button>

        <div className="sidebar-bottom">

          <div className="user-info">
            <div className="user-avatar">
              {user.name?.charAt(0).toUpperCase()}
            </div>

            <div>
              <div className="user-name">
                {user.name}
              </div>

              <div className="user-email">
                {user.email}
              </div>
            </div>
          </div>

          <button
            className="logout-btn"
            onClick={handleLogout}
          >
            Logout
          </button>

        </div>

      </aside>

      {/* ================= MAIN CHAT ================= */}

      <main className="chat-container">

        {/* HEADER */}

        <header className="chat-header">

          <div>
            <h1>AI Assistant</h1>
            <p>Powered by Spring AI</p>
          </div>

          <div className="connection-status">
            <span className="status-dot"></span>
            Connected
          </div>

        </header>

        {/* ================= MESSAGES ================= */}

        <section className="messages">

          {messages.length === 0 ? (

            <div className="welcome">

              <div className="welcome-icon">
                ✦
              </div>

              <h2>
                How can I help you?
              </h2>

              <p>
                Ask me anything and I'll try my best
                to help you.
              </p>

              <div className="suggestions">

                <button
                  onClick={() =>
                    setInput(
                      "Explain dependency injection in Spring Boot"
                    )
                  }
                >
                  Explain Dependency Injection
                </button>

                <button
                  onClick={() =>
                    setInput("What is Spring AI?")
                  }
                >
                  What is Spring AI?
                </button>

                <button
                  onClick={() =>
                    setInput(
                      "Explain microservices in simple terms"
                    )
                  }
                >
                  Explain Microservices
                </button>

              </div>

            </div>

          ) : (

            messages.map((message, index) => (

              <div
                key={index}
                className={`message-row ${message.role}`}
              >

                <div className="avatar">
                  {message.role === "user"
                    ? "You"
                    : "AI"}
                </div>

                <div className="message-content">

                  <div className="message-role">
                    {message.role === "user"
                      ? "You"
                      : "AI Assistant"}
                  </div>

                  <div className="message-text">

                    <ReactMarkdown>
                      {message.content}
                    </ReactMarkdown>

                    {message.role === "assistant" &&
                      loading &&
                      index === messages.length - 1 && (
                        <span className="cursor">
                          ▌
                        </span>
                      )}

                  </div>

                </div>

              </div>

            ))

          )}

          <div ref={messagesEndRef} />

        </section>

        {/* ================= INPUT ================= */}

        <div className="input-area">

          <div className="input-wrapper">

            <textarea
              value={input}
              onChange={(event) =>
                setInput(event.target.value)
              }
              onKeyDown={handleKeyDown}
              placeholder="Message AI Assistant..."
              rows="1"
              disabled={loading}
            />

            <button
              className="send-btn"
              onClick={sendMessage}
              disabled={
                !input.trim() || loading
              }
            >
              ↑
            </button>

          </div>

          <p className="input-hint">
            Press Enter to send · Shift + Enter
            for new line
          </p>

        </div>

      </main>

    </div>
  );
}

export default App;