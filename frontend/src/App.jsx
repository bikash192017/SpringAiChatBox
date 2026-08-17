import { useState } from "react";
import "./App.css";
import ReactMarkdown from "react-markdown";
function App() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const sendMessage = async () => {
    if (!input.trim() || loading) return;

    const userMessage = input.trim();

    setMessages((prev) => [
      ...prev,
      { role: "user", content: userMessage },
    ]);

    setInput("");
    setLoading(true);

    try {
      const response = await fetch("http://localhost:8080/api/chat", {
        method: "POST",
        headers: {
          "Content-Type": "text/plain",
        },
        body: userMessage,
      });

      if (!response.ok) {
        throw new Error("Failed to get response from server");
      }

      const aiResponse = await response.text();

      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: aiResponse },
      ]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: "Sorry, something went wrong. Please try again.",
        },
      ]);

      console.error(error);
    } finally {
      setLoading(false);
    }
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
      <aside className="sidebar">
        <div className="logo">
          <div className="logo-icon">✦</div>
          <span>Spring AI</span>
        </div>

        <button className="new-chat-btn" onClick={startNewChat}>
          <span>＋</span>
          New Chat
        </button>

        <div className="sidebar-footer">
          <span className="status-dot"></span>
          <span>AI Assistant</span>
        </div>
      </aside>

      <main className="chat-container">
        <header className="chat-header">
          <div>
            <h1>AI Assistant</h1>
            <p>Powered by Spring AI</p>
          </div>
        </header>

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
                    setInput("Explain dependency injection in Spring Boot")
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
                    setInput("Explain microservices in simple terms")
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
                    {message.role === "user" ? "You" : "AI Assistant"}
                  </div>

                 <div className="message-text">
  <ReactMarkdown>{message.content}</ReactMarkdown>
</div>
                </div>
              </div>
            ))
          )}

          {loading && (
            <div className="message-row assistant">
              <div className="avatar">AI</div>

              <div className="message-content">
                <div className="message-role">AI Assistant</div>

                <div className="typing">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          )}
        </section>

        <div className="input-area">
          <div className="input-wrapper">
            <textarea
              value={input}
              onChange={(event) => setInput(event.target.value)}
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