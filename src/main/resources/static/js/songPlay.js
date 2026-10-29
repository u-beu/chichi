// "Discord 재생" 버튼: 음성채널 안내 -> "들어갔어요" 확인 후 재생 요청.
// 재생 요청 발행(POST /api/songs/{id}/play)은 TODO.md 이슈 2에서 requestPlayback에 연결한다.
(function () {
    const dialog = document.getElementById('play-dialog');
    const songLabel = document.getElementById('play-dialog-song');
    const toast = document.getElementById('toast');
    let pendingSongId = null;
    let toastTimer;

    function showToast(message) {
        toast.textContent = message;
        toast.classList.add('on');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toast.classList.remove('on'), 2400);
    }

    function requestPlayback(songId) {
        // TODO(이슈 2): POST /api/songs/{songId}/play 호출
        console.log('재생 요청 대기(미연결):', songId);
        showToast('재생 요청 기능은 준비 중이에요.');
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
        pendingSongId = record ? record.dataset.songId || null : null;
        songLabel.textContent = record ? songLabelOf(record) : '';
        dialog.showModal();
    });

    document.getElementById('play-dialog-cancel').addEventListener('click', () => {
        pendingSongId = null;
        dialog.close();
    });

    document.getElementById('play-dialog-confirm').addEventListener('click', () => {
        const songId = pendingSongId;
        pendingSongId = null;
        dialog.close();
        requestPlayback(songId);
    });

    // 배경 클릭 시 닫기
    dialog.addEventListener('click', (event) => {
        if (event.target === dialog) {
            pendingSongId = null;
            dialog.close();
        }
    });
})();
