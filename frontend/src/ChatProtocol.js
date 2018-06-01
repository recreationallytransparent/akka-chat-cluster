
 function message(senderId, chatId, contents) {
  return {
    jsonClass: "ChatProtocol$Message",
    senderId, chatId, contents,
    time: 1111
  }
}

 function join(senderId, chatId) {
  return {
    jsonClass: "ChatProtocol$Join",
    senderId, chatId,
    time: 1111
  }
}

const ChatProtocol = {
  message,
  join
};

export default ChatProtocol;

