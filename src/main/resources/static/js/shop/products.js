/**
 * Submitting the filter form upon category selection
 */
function submitFormWithCategory(selectedCategory) {
    const form = document.getElementById('filterForm');
    const searchInput = document.getElementById('searchQuery');
    const currentSearch = searchInput.value.trim();

    let url = '/shop/products?';
    if (selectedCategory) {
        url += 'category=' + encodeURIComponent(selectedCategory) + '&';
    }
    if (currentSearch) {
        url += 'q=' + encodeURIComponent(currentSearch);
    }
    window.location.href = url;
}