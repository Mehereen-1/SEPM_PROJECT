document.addEventListener('DOMContentLoaded', () => {
  const offersContainer = document.getElementById('offersContainer');
  const offerStats = document.getElementById('offerStats');
  const searchInput = document.getElementById('searchInput');
  const conditionFilter = document.getElementById('conditionFilter');

  let offers = [];

  const escapeHtml = (value) => {
    if (value == null) {
      return '';
    }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const showMessage = (cssClass, message) => {
    offersContainer.className = cssClass;
    offersContainer.innerHTML = `<div>${escapeHtml(message)}</div>`;
  };

  const setConditionOptions = (items) => {
    const uniqueConditions = [...new Set(items.map((offer) => (offer.condition || '').trim()).filter(Boolean))];
    uniqueConditions.sort((a, b) => a.localeCompare(b));

    uniqueConditions.forEach((condition) => {
      const option = document.createElement('option');
      option.value = condition;
      option.textContent = condition;
      conditionFilter.appendChild(option);
    });
  };

  const matchesFilters = (offer) => {
    const query = searchInput.value.trim().toLowerCase();
    const selectedCondition = conditionFilter.value;

    if (selectedCondition && offer.condition !== selectedCondition) {
      return false;
    }

    if (!query) {
      return true;
    }

    const haystack = [
      offer.bookTitle,
      offer.author,
      offer.ownerName,
      offer.note,
      offer.condition
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    return haystack.includes(query);
  };

  const renderOffers = () => {
    const filtered = offers.filter(matchesFilters);

    offerStats.textContent = `${filtered.length} active offer${filtered.length === 1 ? '' : 's'} shown`;

    if (filtered.length === 0) {
      showMessage('empty', 'No active offers match your current filters.');
      return;
    }

    offersContainer.className = 'offers-grid';
    offersContainer.innerHTML = filtered
      .map((offer) => {
        const firstImage = Array.isArray(offer.imageUrls) && offer.imageUrls.length > 0
          ? offer.imageUrls[0]
          : null;

        const galleryHtml = firstImage
          ? `<img src="${escapeHtml(firstImage)}" alt="${escapeHtml(offer.bookTitle || 'Book image')}" loading="lazy">`
          : '<div class="gallery-empty">No Photo</div>';

        const note = offer.note && offer.note.trim() ? offer.note : 'No note provided.';

        return `
          <article class="offer-card">
            <div class="offer-gallery">
              ${galleryHtml}
            </div>
            <div class="offer-body">
              <h3 class="offer-title">${escapeHtml(offer.bookTitle || 'Untitled')}</h3>
              <p class="offer-author">by ${escapeHtml(offer.author || 'Unknown author')}</p>
              <span class="badge-condition">${escapeHtml(offer.condition || 'Unknown')}</span>
              <p class="offer-note">${escapeHtml(note)}</p>
              <div class="offer-meta">
                <span>Owner: ${escapeHtml(offer.ownerName || 'Unknown')}</span>
                <span>${Array.isArray(offer.imageUrls) ? offer.imageUrls.length : 0} image(s)</span>
              </div>
            </div>
          </article>
        `;
      })
      .join('');
  };

  const loadOffers = async () => {
    showMessage('loading', 'Loading active exchange offers...');
    offerStats.textContent = 'Fetching offers...';

    try {
      const response = await fetch('/offers', {
        method: 'GET',
        headers: { Accept: 'application/json' }
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const data = await response.json();
      offers = Array.isArray(data) ? data : [];

      setConditionOptions(offers);
      renderOffers();
    } catch (error) {
      offerStats.textContent = 'Unable to load offers';
      showMessage('error', 'Could not load active offers right now. Please try again in a moment.');
    }
  };

  searchInput.addEventListener('input', renderOffers);
  conditionFilter.addEventListener('change', renderOffers);

  loadOffers();
});
