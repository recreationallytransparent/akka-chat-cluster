import React from 'react'

import ChatRoomDirectory from './ChatRoomDirectory'
import ChatProtocol from './ChatProtocol';

export default class Char extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      name: '',
      nameSubmitted: false,
      roomId: '',
      history: [],
      roomMembership: {},
      message: '',
      rooms: [],
      users: []
    }

    this.installWebSocket = this.installWebSocket.bind(this);
    this.joinChat = this.joinChat.bind(this);
  }

  installWebSocket() {
    if (this.state.name) {
      this.setState({nameSubmitted: true})
      this.ws = new window.WebSocket('ws://desktop-hsi9umq.attlocal.net:8080/chat?id=' + this.state.name);
      this.ws.onmessage = ((event) => {
        try {
          this.handleWSEvent(JSON.parse(event.data))
        } catch (e) {
          console.error("error parsing: ", event)
          console.error(e)
        }
      }).bind(this);
    }
  }

  handleWSEvent(json) {
    console.log('received: ', json);
    const history = this.state.history.concat([json]);
    switch(json.jsonClass.replace('ChatProtocol$', '')) {
      case 'ChatState':
        const {rooms, users, roomMembership} = json;
        return this.setState({rooms, users, roomMembership});
      case 'Message':
        return this.setState({history})
      case 'Join':
      case 'Left':
        return this.setState({history, members: json.members})
      default:
        return;
    }
  }

  wsSend(json) {
    console.log('out: ', json);
    this.ws.send(JSON.stringify(json));
  }

  joinChat(chatRoomId) {
    const join = ChatProtocol.join(this.state.name, chatRoomId);
    this.wsSend(join);
    this.setState({
      roomId: chatRoomId,
      history: [],
    })
  }

  sendMessage(string) {
    const message = ChatProtocol.message(this.state.name, this.state.roomId, string);
    this.wsSend(message);
    this.setState({
      message: ''
    })
  }

  renderJoinedMessage(item, i) {
    return (
      <p key={i}>{item.senderId} joined the chat</p>
    )
  }

  renderChatMessage(item, i) {
    return (
      <p key={i}><strong>{item.senderId}</strong>: {item.contents}</p>
    )
  }

  renderLeftMessage(item, i) {
    return (
      <p key={i}>{item.senderId} left</p>
    )
  }

  renderHistoryItem(item, i) {
    switch(item.jsonClass.replace('ChatProtocol$', '')) {
      case 'Join': return this.renderJoinedMessage(item, i)
      case 'Message': return this.renderChatMessage(item, i)
      case 'Left': return this.renderLeftMessage(item, i)
      default:
        return;
    }
  }

  renderChatWindow() {
    console.log(this.state.history);
    const roomMembers = this.state.roomMembership[this.state.roomId] || [];
    return (
      <div style={style.chat}>
        <a onClick={() => this.setState({roomId: ''})}>&lt; Back to room list</a>
        <h4>{this.state.roomId}</h4>
        <p><strong>members</strong>: {roomMembers.join(', ')}</p>

        <p>logged in as: {this.state.name}</p>
        <hr />

        <div style={style.display}>
          {this.state.history.map((p, i) => this.renderHistoryItem(p, i))}
        </div>

        <hr />

        <form onSubmit={(e) => e.preventDefault()}>
          <input type="text" value={this.state.message} onInput={e => this.setState({message: e.target.value})} />
          <button type="submit" onClick={this.sendMessage.bind(this, this.state.message)}>Submit</button>
        </form>
      </div>
    )
  }

  renderNameInput() {
    return (
      <div>
        <form>
          <p>Enter Username: </p>
          <input name="name" value={this.state.name} onInput={e => this.setState({name: e.target.value})} />
          <button type="submit" value="Submit" onClick={this.installWebSocket}>Submit</button>
        </form>
      </div>
    )
  }

  renderChatRoomDirectory() {
    return (
      <ChatRoomDirectory
        onNewRoomSubmit={this.joinChat}
        onRoomClick={this.joinChat}
        rooms={this.state.rooms}
        users={this.state.users}
      />
    )
  }

  render() {
    if (!this.state.nameSubmitted) {
      return this.renderNameInput();
    } else if (!this.state.roomId) {
      return this.renderChatRoomDirectory();
    } else {
      return this.renderChatWindow();
    }
  }
}

const style = {
  chat: {
    width: 600,
    margin: '0 auto',
    textAlign: 'left',

  },
  display: {
    border: '1px solid black',
    padding: 15,
    height: 700,
    overflow: 'scroll'
  }
}