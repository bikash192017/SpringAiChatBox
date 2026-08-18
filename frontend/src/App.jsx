import { useEffect, useRef, useState } from "react";
import "./App.css";
import ReactMarkdown from "react-markdown";

function App() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const socketRef = useRef(null);
  const messagesEndRef = useRef(null);

  // Automatically scroll to the latest message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages]);

  // Create WebSocket connection when component loads
  useEffect(() => {
    const socket = new WebSocket("ws://localhost:8080/ws/chat");

    socketRef.current = socket;

    socket.onopen = () => {
      console.log("WebSocket connected");
    };

    socket.onmessage = (event) => {
      const chunk = event.data;

      console.log("Received chunk:", chunk);

      // Backend tells us that streaming is finished
      if (chunk === "[DONE]") {
        console.log("AI response completed");
        setLoading(false);
        return;
      }

      // Add the received chunk to the current AI message
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

    // Close connection when component is removed
    return () => {
      if (
        socket.readyState === WebSocket.OPEN ||
        socket.readyState === WebSocket.CONNECTING
      ) {
        socket.close();
      }
    };
  }, []);

  const sendMessage = () => {
    if (!input.trim() || loading) return;

    const userMessage = input.trim();

    // Make sure WebSocket is connected
    if (
      !socketRef.current ||
      socketRef.current.readyState !== WebSocket.OPEN
    ) {
      console.error("WebSocket is not connected");
      return;
    }

    // Add user message and empty AI message
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

    // Send prompt to Spring Boot through WebSocket
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

  return (
    <div className="app">
      {/* Sidebar */}
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

        <div className="sidebar-footer">
          <span className="status-dot"></span>
          <span>AI Assistant</span>
        </div>
      </aside>

      {/* Main Chat */}
      <main className="chat-container">
        {/* Header */}
        <header className="chat-header">
          <div>
            <h1>AI Assistant</h1>
            <p>Powered by Spring AI</p>
          </div>
        </header>

        {/* Messages */}
        <section className="messages">
          {messages.length === 0 ? (
            <div className="welcome">
              <div className="welcome-icon">✦</div>

              <h2>How can I help you?</h2>

              <p>
                Ask me anything and I'll try my best to help you.
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
                  {message.role === "user" ? "You" : "AI"}
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

                    {/* Streaming cursor */}
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

          {/* Auto-scroll target */}
          <div ref={messagesEndRef} />
        </section>

        {/* Input */}
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
              disabled={!input.trim() || loading}
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