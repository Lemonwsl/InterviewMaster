<template>
  <v-app>
    <v-main>
      <v-container>
        <v-row>
          <v-col cols="12" sm="8" offset-sm="2">
            <v-card class="pa-3">
              <v-card-title class="text-h5">Ai interviewer</v-card-title>
              <v-card-text class="chat-container" ref="chatContainer">
                <div class="messages">
                  <div v-for="msg in messages" :key="msg.id" :class="{'message-user': msg.sender === 'user', 'message-bot': msg.sender === 'bot'}">
                    <div class="message-content">{{ msg.text }}</div>
                  </div>
                </div>
              </v-card-text>
              <v-card-actions>
                <v-text-field
                  v-model="input"
                  label="Type a message..."
                  outlined
                  dense
                  class="flex-grow-1"
                  @keyup.enter="sendMessage"
                ></v-text-field>
                <v-btn color="primary" @click="sendMessage">Send</v-btn>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      // user's input (aka interviewer's input, will be text the whole time)
      input: '',
      // list to store all the inputs to send to backends
      messages: [], 
    };
  },
  methods: {
    async sendMessage() {

      // if the input is empty but clicked/entered send, ignore it
      if (!this.input.trim()) return;

      // add the input to the list
      this.messages.push({
        // use Date as unique identifier, so make it id
        id: Date.now(),
        // text user entered
        text: this.input,
        // recognize as user
        sender: 'user'
      });

      try {
        // send request to backend - here please change '/api/chat' to what you created
        const response = await axios.post('/api/chat', {
          message: this.input,
        });

        // callbacks from backend
        this.messages.push({
          id: Date.now(),
          // chatgpt's response, we might need to combine the "analyzer" to this when we integrate them, so here might not neccessary need to be reply
          text: response.data.reply,
          // because to make it secure, the chatgpt call will be put at the backend, so i set the sender as 'bot'
          sender: 'bot'
        });
      } catch (error) {
        console.error("Failed to send message: ", error);
      }

      // clear the input after clicking "send"
      this.input = '';
    },
  },
};
</script>




<style scoped>
.chat-container {
  max-height: 60vh;
  overflow-y: auto;
}
.messages {
  display: flex;
  flex-direction: column;
}
.message-user {
  align-self: flex-end;
  background-color: #f0f0f0;
}
.message-bot {
  align-self: flex-start;
  background-color: #e0e0e0;
}
.message-content {
  margin: 5px;
  padding: 10px;
  border-radius: 10px;
}
</style>