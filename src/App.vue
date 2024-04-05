<template>
  <v-app>
    <v-main>
      <v-container fluid>
        <v-row>
          <v-col cols="12">
            <v-card class="pa-3 mx-auto chat-width-modifier">
              <v-card-title class="text-h4">Ai interviewer</v-card-title>
              <v-card-text class="chat-container" ref="chatContainer">
                <div class="messages" ref="messagesContainer">
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    :class="{
                      'message-user': msg.sender === 'user',
                      'message-bot': msg.sender === 'bot',
                    }"
                  >
                    <div class="message-content">{{ msg.text }}</div>
                  </div>
                </div>
              </v-card-text>
              <v-card-actions>
                <v-row no-gutters>
                  <v-col cols="2">
                    <v-file-input
                      v-model="file"
                      label="Upload resume"
                      outlined
                      dense
                      class="flex-grow-1"
                      @change="handleFileUpload"
                      accept=".pdf"
                      prepend-icon=""
                    ></v-file-input>
                  </v-col>
                  <v-col cols="9">
                    <v-text-field
                      v-model="input"
                      label="Type a message..."
                      outlined
                      dense
                      class="flex-grow-1"
                      @keyup.enter="sendMessage"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="1">
                    <v-btn
                      color="primary"
                      outlined
                      class="mt-3"
                      @click="sendMessage"
                    >
                      Send
                    </v-btn>
                  </v-col>
                </v-row>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import axios from "axios";

export default {
  data() {
    return {
      // user's input (aka interviewer's input, will be text the whole time)
      input: "",
      // list to store all the inputs to send to backends
      messages: [],
      // store the resume here (pdf)
      file: null,
    };
  },
  methods: {
    async sendMessage() {
      // if "type a message" and "add pdf" file are both empty, i will add speaker option
      if (!this.input.trim() && !this.file) return;

      const formData = new FormData();

      if (this.file) {
        formData.append("file", this.file);
      }
      	
      if (this.input.trim()) {
        formData.append("message", this.input);
      }

      try {
        // axios can set the content-type automatically
        const response = await axios.post("http://localhost:3000/api/pdf", formData, {
          withCredentials: true,
        });

        this.messages.push({
          id: Date.now(),
          text: response.data.reply,
          sender: "bot",
        });

        this.input = "";
        this.file = null;
      } catch (error) {
        console.error("Failed to send message: ", error);
      }
    },

    handleFileUpload(file) {
      this.file = file;
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer;
        container.scrollTop = container.scrollHeight;
      });
    },
  },
  updated() {
    this.scrollToBottom();
  },
};
</script>

<style scoped>
.chat-container {
  height: 77.5vh;
  overflow-y: auto;
}
.messages {
  display: flex;
  flex-direction: column;
}
.message-user {
  max-width: 85%;
  font-size: 0.9rem;
  border-radius: 6px;
  align-self: flex-end;
  background-color: #4a76a8;
  color: #ffffff;
  margin-bottom: 4px;
}
.message-bot {
  max-width: 85%;
  font-size: 0.9rem;
  border-radius: 6px;
  align-self: flex-start;
  background-color: #8d8d8d;
  color: #ffffff;
  margin-bottom: 4px;
}
.message-content {
  margin: 5px;
  padding: 10px;
  border-radius: 12;
}
.v-container {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-width: 960px;
  margin-left: auto;
  margin-right: auto;
}
.chat-width-modifier {
  border-radius: 15px;
  box-shadow: 0 0px 48px rgba(0, 0, 0, 0.2);
  max-width: 960px;
}

/* bigger screen use bigger font */
@media (min-width: 600px) {
  .message-user,
  .message-bot {
    font-size: 1.2rem;
  }
}
</style>
