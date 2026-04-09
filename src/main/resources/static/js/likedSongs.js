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
                    container.innerHTML = `
                <div style="text-align: center; margin-top: 50px; color: #999;">
                    좋아요 표시한 곡이 없습니다.
                </div>
                `;
                }
            }, 300);
        }

    }
}