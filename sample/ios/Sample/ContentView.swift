//
//  ContentView.swift
//  Sample
//
//  Created by Brahyam Meneses on 20.06.25.
//

import SwiftUI
import Gateway

struct Message: Identifiable {
    let id = UUID()
    let text: String
    let isFromUser: Bool
}

struct ContentView: View {
    @State private var messages: [Message] = []
    @State private var inputText: String = ""
    @State private var isLoading: Bool = false
    
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
        
        // Simulate API call (replace with actual Gateway integration)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            let response = "This is a simulated response. In a real implementation, this would be replaced with an actual API call to the Gateway service using the Kotlin Multiplatform client."
            messages.append(Message(text: response, isFromUser: false))
            isLoading = false
        }
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
