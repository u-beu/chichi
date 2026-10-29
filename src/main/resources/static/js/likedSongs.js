function removeRecordIfUnliked(button) {
    if (!button.classList.contains('liked')) {
        const record = button.closest('.record');
        if (record) {
            record.style.opacity = '0';
            record.style.transform = 'translateX(-10px)';
            record.style.transition = 'all 0.3s ease-out';

            setTimeout(() => {
                record.remove();

                const container = document.getElementById('liked-song-container');
                const otherRecords = container.getElementsByClassName('record');

                if (otherRecords.length === 0) {
                    const empty = document.createElement('div');
                    empty.className = 'empty-msg';
                    empty.textContent = '좋아요 표시한 곡이 없습니다.';
                    container.replaceChildren(empty);
                }
            }, 300);
        }

    }
}