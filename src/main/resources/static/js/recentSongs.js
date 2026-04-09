const eventSource = new EventSource('/connect');

eventSource.addEventListener("recentSongUpdate", (event) => {
    const song = JSON.parse(event.data);
    const container = document.getElementById('song-list-container');

    const noMsg = document.getElementById('no-records-msg');
    if (noMsg) {
        noMsg.remove();
    }

    const existingRecord = container.querySelector(`[data-song-id="${song.songId}"]`);
    if (existingRecord) {
        existingRecord.remove();
    }

    const newRecord = document.createElement('div');
    newRecord.className = 'record';
    newRecord.setAttribute('data-song-id', song.songId);

    const songImage = song.image != null && song.image != 'null' ? song.image : '/images/basicProfileCat.png';
    const likedClass = song.isLiked ? 'liked' : '';
    newRecord.innerHTML = `
        <div class="record-left">
            <img src="${songImage}" alt="앨범" class="album-cover" />
            <div class="song-info">
                <div class="song-title">${song.title}</div>
                <div class="song-uploader">${song.uploader}</div>
            </div>
        </div>
        <button class="like-btn ${likedClass}" data-song-id="${song.songId}" onclick="toggleLike(this)"></button>
    `;

    container.prepend(newRecord);

    const records = container.getElementsByClassName('record');
    if (records.length > 30) {
        container.removeChild(records[records.length - 1]);
    }
});

eventSource.onerror = function () {
    console.log("SSE 연결 끊겼습니다. 재연결을 시도합니다.");
};
