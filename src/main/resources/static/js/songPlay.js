// "Discord 재생" 버튼: 음성채널 안내 -> "들어갔어요" 확인 후 재생 요청(POST /api/songs/{id}/play).
(function () {
    const dialog = document.getElementById('play-dialog');
    const songLabel = document.getElementById('play-dialog-song');
    const toast = document.getElementById('toast');
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    let pendingSong = null;
    let toastTimer;

    function showToast(message) {
        toast.textContent = message;
        toast.classList.add('on');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toast.classList.remove('on'), 2400);
    }

    function requestPlayback(song) {
        fetch(`/api/songs/${song.songId}/play`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({title: song.title, uploader: song.uploader})
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error('서버 응답 실패');
                }
                showToast('Discord로 재생을 요청했어요.');
            })
            .catch((err) => {
                console.error('재생 요청 중 오류:', err);
                showToast('재생 요청에 실패했어요.');
            });
    }

    // 카드의 곡 정보 (제목/가수는 서버로 보낼 검색어로 사용)
    function songOf(record) {
        const title = record.querySelector('.song-title');
        const uploader = record.querySelector('.song-uploader');
        return {
            songId: record.dataset.songId || null,
            title: title ? title.textContent.trim() : '',
            uploader: uploader ? uploader.textContent.trim() : ''
        };
    }

    // "가수 - 제목" 형식 (가수 정보가 없으면 제목만)
    function songLabelOf(record) {
        const title = record.querySelector('.song-title');
        const uploader = record.querySelector('.song-uploader');
        const parts = [uploader, title]
            .map((el) => (el ? el.textContent.trim() : ''))
            .filter((text) => text !== '');
        return parts.join(' - ');
    }

    // SSE로 추가되는 카드에도 동작하도록 이벤트 위임 사용
    document.addEventListener('click', (event) => {
        const playBtn = event.target.closest('.play-btn');
        if (!playBtn) {
            return;
        }
        const record = playBtn.closest('.record');
        pendingSong = record ? songOf(record) : null;
        songLabel.textContent = record ? songLabelOf(record) : '';
        dialog.showModal();
    });

    document.getElementById('play-dialog-cancel').addEventListener('click', () => {
        pendingSong = null;
        dialog.close();
    });

    document.getElementById('play-dialog-confirm').addEventListener('click', () => {
        const song = pendingSong;
        pendingSong = null;
        dialog.close();
        if (!song || !song.songId || !song.title) {
            showToast('재생할 곡 정보를 찾을 수 없어요.');
            return;
        }
        requestPlayback(song);
    });

    // 배경 클릭 시 닫기
    dialog.addEventListener('click', (event) => {
        if (event.target === dialog) {
            pendingSong = null;
            dialog.close();
        }
    });
})();
