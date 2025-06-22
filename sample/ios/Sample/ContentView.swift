//
//  ContentView.swift
//  Sample
//
//  Created by Brahyam Meneses on 20.06.25.
//

import SwiftUI
import Gateway
import Foundation

struct Message: Identifiable {
    let id = UUID()
    let text: String
    let isFromUser: Bool
}

struct ContentView: View {
    @State private var messages: [Message] = []
    @State private var inputText: String = ""
    @State private var isLoading: Bool = false
    
    // Gateway OpenAI service instance
    private let openAIService: OpenAIService = Gateway.shared.protectedOpenAIService(partialKey: <#T##String#>, serviceURL: <#T##String#>, logging: <#T##Openai_clientLoggingConfig#>, timeout: <#T##Openai_coreTimeout#>, organization: <#T##String?#>, headers: <#T##[String : String]#>, proxy: <#T##(any Openai_clientProxyConfig)?#>, retry: <#T##Openai_clientRetryStrategy#>) {
        // Replace these with your actual Gateway credentials
        let partialKey = "your-partial-key-here"
        let serviceURL = "https://your-gateway-service-url.com"
        
        return Gateway.protectedOpenAIService(
            partialKey: partialKey,
            serviceURL: serviceURL
        )
    }()
    
    var body: some View {
        VStack {
            // Chat messages
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(messages) { message in
                        MessageBubble(message: message)
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 1)
            
            // Loading indicator
            if isLoading {
                ProgressView()
                    .scaleEffect(1.2)
                    .padding()
            }
            
            // Input area
            HStack {
                TextField("Type a message...", text: $inputText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(isLoading)
                
                Button(action: sendMessage) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(.white)
                        .padding(8)
                        .background(Color.blue)
                        .clipShape(Circle())
                }
                .disabled(inputText.isEmpty || isLoading)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
        .navigationTitle("Chat")
    }
    
    private func sendMessage() {
        guard !inputText.isEmpty && !isLoading else { return }
        
        let userMessage = Message(text: inputText, isFromUser: true)
        messages.append(userMessage)
        
        let currentInput = inputText
        inputText = ""
        isLoading = true
        
        // Send message to OpenAI via Gateway
        Task {
            do {
                let response = try await sendToOpenAI(message: currentInput)
                await MainActor.run {
                    messages.append(Message(text: response, isFromUser: false))
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    messages.append(Message(text: "Error: \(error.localizedDescription)", isFromUser: false))
                    isLoading = false
                }
            }
        }
    }
    
    private func sendToOpenAI(message: String) async throws -> String {
        // Convert existing messages to OpenAI format
        let chatMessages = messages.map { msg in
            ChatMessage(
                role: msg.isFromUser ? Role.User : Role.Assistant,
                content: msg.text
            )
        }
        
        // Add the current user message
        let allMessages = chatMessages + [ChatMessage(role: Role.User, content: message)]
        
        // Create chat completion request
        let request = ChatCompletionRequest(
            model: ModelId(id: "gpt-3.5-turbo"),
            messages: allMessages,
            temperature: 0.7,
            maxTokens: 1000
        )
        
        // Send request to OpenAI via Gateway
        let response = try await openAIService.chatCompletions(request)
        
        // Extract the response text
        guard let firstChoice = response.choices.first,
              let content = firstChoice.message.content else {
            throw NSError(domain: "OpenAI", code: -1, userInfo: [NSLocalizedDescriptionKey: "No response content received"])
        }
        
        return content
    }
}

struct MessageBubble: View {
    let message: Message
    
    var body: some View {
        HStack {
            if message.isFromUser {
                Spacer()
            }
            
            Text(message.text)
                .padding(12)
                .background(message.isFromUser ? Color.blue : Color.gray.opacity(0.2))
                .foregroundColor(message.isFromUser ? .white : .primary)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .frame(maxWidth: 300, alignment: message.isFromUser ? .trailing : .leading)
            
            if !message.isFromUser {
                Spacer()
            }
        }
    }
}

#Preview {
    NavigationView {
        ContentView()
    }
}
