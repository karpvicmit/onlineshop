/**
 * Submitting the filter form upon category selection
 */
function submitFormWithCategory(selectedCategory) {
    const form = document.getElementById('filterForm');
    const searchInput = document.getElementById('searchQuery');
    const sortSelect = document.getElementById('sortSelect');
    const currentSearch = searchInput.value.trim();
    const currentSort = sortSelect.value;

    let url = '/shop/products?';

    if (selectedCategory) {
        url += 'category=' + encodeURIComponent(selectedCategory) + '&';
    }
    if (currentSearch) {
        url += 'q=' + encodeURIComponent(currentSearch) + '&';
    }
    if (currentSort && currentSort !== 'date_desc') {
        url += 'sort=' + encodeURIComponent(currentSort);
    }

    window.location.href = url;
}

/**
 * Submitting the filter form upon sort selection
 */
function submitFormWithSort(selectedSort) {
    const form = document.getElementById('filterForm');
    const searchInput = document.getElementById('searchQuery');
    const categorySelect = document.getElementById('categorySelect');
    const currentSearch = searchInput.value.trim();
    const currentCategory = categorySelect.value;

    let url = '/shop/products?';

    if (currentCategory) {
        url += 'category=' + encodeURIComponent(currentCategory) + '&';
    }
    if (currentSearch) {
        url += 'q=' + encodeURIComponent(currentSearch) + '&';
    }
    if (selectedSort && selectedSort !== 'date_desc') {
        url += 'sort=' + encodeURIComponent(selectedSort);
    }

    window.location.href = url;
}