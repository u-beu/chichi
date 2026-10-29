const eventSource = new EventSource('/connect');

const DEFAULT_IMAGE = '/images/basicProfileCat.png';
// 고정 마크업(사용자 입력 없음)이라 innerHTML 사용 가능
const PLAY_ICON = '<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">'
    + '<path d="M7 4.5v15a1 1 0 0 0 1.5.9l12-7.5a1 1 0 0 0 0-1.7l-12-7.5A1 1 0 0 0 7 4.5Z"/></svg>';

function createRecord(song) {
    const record = document.createElement('div');
    record.className = 'record';
    record.dataset.songId = song.songId;

    const left = document.createElement('div');
    left.className = 'record-left';

    const cover = document.createElement('img');
    cover.className = 'album-cover';
    cover.alt = '앨범';
    cover.src = song.image != null && song.image !== 'null' && song.image !== '' ? song.image : DEFAULT_IMAGE;

    const info = document.createElement('div');
    info.className = 'song-info';

    const title = document.createElement('div');
    title.className = 'song-title';
    title.textContent = song.title;

    const uploader = document.createElement('div');
    uploader.className = 'song-uploader';
    uploader.textContent = song.uploader;

    info.append(title, uploader);
    left.append(cover, info);

    const playBtn = document.createElement('button');
    playBtn.type = 'button';
    playBtn.className = 'play-btn';
    playBtn.setAttribute('aria-label', 'Discord에서 재생');
    playBtn.innerHTML = PLAY_ICON;
    const playLabel = document.createElement('span');
    playLabel.textContent = 'Discord 재생';
    playBtn.append(playLabel);

    const likeBtn = document.createElement('button');
    likeBtn.className = song.isLiked ? 'like-btn liked' : 'like-btn';
    likeBtn.dataset.songId = song.songId;
    likeBtn.setAttribute('aria-label', '좋아요');
    likeBtn.addEventListener('click', () => toggleLike(likeBtn));

    record.append(left, playBtn, likeBtn);
    return record;
}

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

    container.prepend(createRecord(song));

    const records = container.getElementsByClassName('record');
    if (records.length > 30) {
        container.removeChild(records[records.length - 1]);
    }
});

eventSource.onerror = function () {
    console.log("SSE 연결 끊겼습니다. 재연결을 시도합니다.");
};
