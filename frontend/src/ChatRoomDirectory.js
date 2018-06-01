import React from 'react'

export default class ChatRoomDirectory extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      roomName: '',
    };
    this.onNewRoomSubmit = this.onNewRoomSubmit.bind(this);
  }

  onNewRoomSubmit(e) {
    e.preventDefault();
    this.props.onNewRoomSubmit(this.state.roomName)
  }
  render() {
    return (
      <div>

        <div>
          <h4>You can also enter a new room:</h4>
          <form onSubmit={this.onNewRoomSubmit}>
            <input type="text" name="roomName" value={this.state.roomName} onChange={(e) => this.setState({roomName: e.target.value})} />
            <input type="submit" value="Submit" />
          </form>
        </div>

        <div>
          <h4>Users online:</h4>
          <p>{this.props.users.join(', ')}</p>
        </div>
        <div>
          <h4>Rooms:</h4>
          <ul>
            {this.props.rooms.map((room, k) => <li style={style.room} key={k} onClick={() => this.props.onRoomClick(room)}>{room}</li>)}
          </ul>
        </div>
      </div>
    );
  }
}

const style = {
  room: {
    cursor: 'pointer',
    padding: 25
  }
};
