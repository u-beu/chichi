function toggleLike(btn) {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    const songId = btn.dataset.songId;

    fetch(`/api/songs/${songId}/like`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [header]: token
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 실패');
            }
            return response.json();
        })
        .then(result => {
            const checkIsLiked = result.data.isLiked;
            if (checkIsLiked) {
                btn.classList.add('liked');
            } else {
                btn.classList.remove('liked');
            }
        }).catch(err => {
            console.error('좋아요 처리 중 오류:', err);
            alert("좋아요 처리에 실패했습니다.");
        });
}