/**
 * Quantity selector for product detail page
 */
function changeQty(delta) {
    const input = document.getElementById('quantity');
    if (!input) return;
    const min = parseInt(input.min) || 1;
    const max = parseInt(input.max) || 99;
    let value = parseInt(input.value) || 1;
    value = Math.max(min, Math.min(max, value + delta));
    input.value = value;
}