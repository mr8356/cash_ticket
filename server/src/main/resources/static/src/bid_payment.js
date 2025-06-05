import TossPayments from '@tosspayments/tosspayments-sdk';

document.addEventListener('DOMContentLoaded', () => {
    /* ❶ Thymeleaf data-attr로 주입 */
    const bidForm   = document.querySelector('#bidForm');
    const concertId = bidForm.dataset.concert;           // ex) "1"
    const bidInput  = document.querySelector('#bidAmount');
    const payBtn    = document.querySelector('#payBtn');

    payBtn.addEventListener('click', async () => {
        const bidAmount = Number(bidInput.value);

        /* ❷ 주문(입찰) 생성 */
        const res = await fetch('/api/auction/orders', {
            method : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body   : JSON.stringify({
                concertId,
                bidAmount
            })
        });
        if (!res.ok) {
            const msg = await res.text();        // validateBid() 예외 메시지
            return alert(msg);
        }
        const { orderId, amount } = await res.json();

        /* ❸ Toss SDK 초기화 & 결제창 호출 */
        const toss = TossPayments(import.meta.env.VITE_TOSS_CLIENT_KEY);

        toss.requestPayment('카드', {
            amount,
            orderId,
            orderName : '콘서트 경매 입찰',
            successUrl: `${location.origin}/auction/payment/success`,
            failUrl   : `${location.origin}/auction/payment/fail`
        });
    });
});
