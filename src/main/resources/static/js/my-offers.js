document.addEventListener('DOMContentLoaded', () => {
  const offersContainer = document.getElementById('offersContainer');
  const statusBanner = document.getElementById('statusBanner');

  const editModal = document.getElementById('editModal');
  const editCondition = document.getElementById('editCondition');
  const editNote = document.getElementById('editNote');
  const cancelEditBtn = document.getElementById('cancelEditBtn');
  const saveEditBtn = document.getElementById('saveEditBtn');

  let offers = [];
  let editingOfferId = null;

  const escapeHtml = (value) => {
    if (value == null) {
      return '';
    }
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  const authHeaders = (includeJson = false) => {
    const headers = { Accept: 'application/json' };
    if (includeJson) {
      headers['Content-Type'] = 'application/json';
    }

    const token = localStorage.getItem('token');
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    return headers;
  };

  const readErrorMessage = async (response) => {
    try {
      const payload = await response.json();
      if (payload && payload.message) {
        return payload.message;
      }
    } catch (_) {
      // ignored on purpose
    }
    return `Request failed with status ${response.status}`;
  };

  const setStatus = (type, message) => {
    statusBanner.className = `status-banner ${type}`;
    statusBanner.textContent = message;
  };

  const clearStatus = () => {
    statusBanner.className = 'status-banner';
    statusBanner.textContent = '';
  };

  const dateText = (value) => {
    if (!value) {
      return 'Unknown time';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString();
  };

  const openEditModal = (offer) => {
    editingOfferId = offer.offerId;
    editCondition.value = offer.condition || 'Good';
    editNote.value = offer.note || '';
    editModal.classList.add('show');
    editModal.setAttribute('aria-hidden', 'false');
  };

  const closeEditModal = () => {
    editingOfferId = null;
    editModal.classList.remove('show');
    editModal.setAttribute('aria-hidden', 'true');
  };

  const renderOffers = () => {
    if (!Array.isArray(offers) || offers.length === 0) {
      offersContainer.innerHTML = '<div class="empty">No offers created yet. Use Share This Book from Browse Books.</div>';
      return;
    }

    offersContainer.innerHTML = offers.map((offer) => {
      const firstImage = Array.isArray(offer.imageUrls) && offer.imageUrls.length > 0
        ? offer.imageUrls[0]
        : null;
      const image = firstImage
        ? `<img src="${escapeHtml(firstImage)}" alt="${escapeHtml(offer.bookTitle || 'Book image')}" loading="lazy">`
        : '<span>No Image</span>';

      const note = offer.note && offer.note.trim() ? offer.note : 'No note provided.';

      return `
        <article class="offer-card">
          <div class="offer-cover">${image}</div>
          <div class="offer-body">
            <p class="offer-title">${escapeHtml(offer.bookTitle || 'Untitled')}</p>
            <div class="pill">${escapeHtml(offer.status || 'ACTIVE')}</div>
            <p class="offer-meta">
              Author: ${escapeHtml(offer.author || 'Unknown')}<br>
              Condition: ${escapeHtml(offer.condition || 'Unknown')}<br>
              Created: ${escapeHtml(dateText(offer.createdAt))}<br>
              Note: ${escapeHtml(note)}
            </p>
            <div class="actions">
              <button class="btn btn-edit" data-edit-id="${escapeHtml(offer.offerId)}">Modify</button>
              <button class="btn btn-delete" data-delete-id="${escapeHtml(offer.offerId)}">Delete</button>
            </div>
          </div>
        </article>
      `;
    }).join('');
  };

  const loadOffers = async () => {
    const response = await fetch('/offers/my', {
      method: 'GET',
      headers: authHeaders(false)
    });

    if (response.status === 401) {
      offers = [];
      renderOffers();
      setStatus('error', 'Log in to view your offers.');
      return;
    }

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }

    const payload = await response.json();
    offers = Array.isArray(payload) ? payload : [];
    renderOffers();
  };

  const saveEdit = async () => {
    if (!editingOfferId) {
      return;
    }

    const condition = editCondition.value;
    const note = editNote.value.trim();

    const response = await fetch(`/offers/${editingOfferId}`, {
      method: 'PUT',
      headers: authHeaders(true),
      body: JSON.stringify({ condition, note })
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }
  };

  const deleteOffer = async (offerId) => {
    const response = await fetch(`/offers/${offerId}`, {
      method: 'DELETE',
      headers: authHeaders(false)
    });

    if (!response.ok) {
      throw new Error(await readErrorMessage(response));
    }
  };

  offersContainer.addEventListener('click', async (event) => {
    const editBtn = event.target.closest('[data-edit-id]');
    if (editBtn) {
      const offerId = Number(editBtn.getAttribute('data-edit-id'));
      const offer = offers.find((item) => item.offerId === offerId);
      if (offer) {
        openEditModal(offer);
      }
      return;
    }

    const deleteBtn = event.target.closest('[data-delete-id]');
    if (deleteBtn) {
      const offerId = Number(deleteBtn.getAttribute('data-delete-id'));
      if (!offerId) {
        return;
      }

      if (!window.confirm('Delete this offer? This cannot be undone.')) {
        return;
      }

      clearStatus();
      try {
        await deleteOffer(offerId);
        setStatus('success', 'Offer deleted successfully.');
        await loadOffers();
      } catch (error) {
        setStatus('error', error.message || 'Failed to delete offer.');
      }
    }
  });

  cancelEditBtn.addEventListener('click', closeEditModal);

  saveEditBtn.addEventListener('click', async () => {
    clearStatus();
    saveEditBtn.disabled = true;
    try {
      await saveEdit();
      closeEditModal();
      setStatus('success', 'Offer updated successfully.');
      await loadOffers();
    } catch (error) {
      setStatus('error', error.message || 'Failed to update offer.');
    } finally {
      saveEditBtn.disabled = false;
    }
  });

  editModal.addEventListener('click', (event) => {
    if (event.target === editModal) {
      closeEditModal();
    }
  });

  (async () => {
    clearStatus();
    try {
      await loadOffers();
    } catch (error) {
      setStatus('error', error.message || 'Failed to load your offers.');
    }
  })();
});
