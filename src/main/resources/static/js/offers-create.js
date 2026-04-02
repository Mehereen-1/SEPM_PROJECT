document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('createOfferForm');
  const bookSelect = document.getElementById('bookId');
  const conditionSelect = document.getElementById('condition');
  const noteInput = document.getElementById('note');
  const imagesInput = document.getElementById('images');
  const statusBox = document.getElementById('statusBox');
  const imagePreviewList = document.getElementById('imagePreviewList');
  const selectedBookInfo = document.getElementById('selectedBookInfo');
  const submitBtn = document.getElementById('submitBtn');
  const resetBtn = document.getElementById('resetBtn');

  let books = [];

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

  const setStatus = (type, message) => {
    statusBox.className = `status-box ${type}`;
    statusBox.textContent = message;
  };

  const clearStatus = () => {
    statusBox.className = 'status-box';
    statusBox.textContent = '';
  };

  const setSubmitting = (isSubmitting) => {
    submitBtn.disabled = isSubmitting;
    submitBtn.textContent = isSubmitting ? 'Creating Offer...' : 'Create Offer';
  };

  const populateBooks = (items) => {
    books = Array.isArray(items) ? items : [];

    bookSelect.innerHTML = '<option value="">Select a book</option>';

    books.forEach((book) => {
      const option = document.createElement('option');
      option.value = book.bookId;
      option.textContent = `${book.title} - ${book.author}`;
      bookSelect.appendChild(option);
    });

    if (books.length === 0) {
      bookSelect.innerHTML = '<option value="">No books available</option>';
    }
  };

  const refreshSelectedBookInfo = () => {
    const selectedId = bookSelect.value;
    const selectedBook = books.find((book) => book.bookId === selectedId);

    if (!selectedBook) {
      selectedBookInfo.style.display = 'none';
      selectedBookInfo.textContent = '';
      return;
    }

    selectedBookInfo.style.display = 'block';
    selectedBookInfo.innerHTML = `
      <strong>${escapeHtml(selectedBook.title)}</strong>
      by ${escapeHtml(selectedBook.author || 'Unknown author')}
    `;
  };

  const refreshImagePreview = () => {
    const files = Array.from(imagesInput.files || []);
    imagePreviewList.innerHTML = '';

    if (files.length === 0) {
      return;
    }

    files.forEach((file) => {
      const li = document.createElement('li');
      li.textContent = `${file.name} (${Math.ceil(file.size / 1024)} KB)`;
      imagePreviewList.appendChild(li);
    });
  };

  const uploadImages = async (offerId, files) => {
    if (!files || files.length === 0) {
      return;
    }

    const formData = new FormData();
    Array.from(files).forEach((file) => formData.append('files', file));

    const response = await fetch(`/offers/${offerId}/images`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      let message = 'Offer created, but image upload failed.';
      try {
        const data = await response.json();
        if (data && data.message) {
          message = data.message;
        }
      } catch (error) {
        // Keep default message
      }
      throw new Error(message);
    }
  };

  const createOffer = async () => {
    const payload = {
      bookId: bookSelect.value,
      condition: conditionSelect.value,
      note: noteInput.value.trim()
    };

    const response = await fetch('/offers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    let data = null;
    try {
      data = await response.json();
    } catch (error) {
      data = null;
    }

    if (!response.ok) {
      throw new Error((data && data.message) ? data.message : 'Failed to create offer.');
    }

    return data;
  };

  const validate = () => {
    if (!bookSelect.value) {
      setStatus('error', 'Please select a book from the catalog.');
      return false;
    }

    if (!conditionSelect.value) {
      setStatus('error', 'Please select the condition of your book copy.');
      return false;
    }

    return true;
  };

  const loadBooks = async () => {
    try {
      const response = await fetch('/books/browse', {
        method: 'GET',
        headers: { Accept: 'application/json' }
      });

      if (!response.ok) {
        throw new Error('Unable to load books.');
      }

      const data = await response.json();
      populateBooks(data);
    } catch (error) {
      bookSelect.innerHTML = '<option value="">Unable to load books</option>';
      setStatus('error', 'Could not load books. Refresh and try again.');
    }
  };

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearStatus();

    if (!validate()) {
      return;
    }

    setSubmitting(true);

    try {
      const createdOffer = await createOffer();
      const offerId = createdOffer && createdOffer.id;

      if (!offerId) {
        throw new Error('Offer created but no offer ID was returned.');
      }

      await uploadImages(offerId, imagesInput.files);

      setStatus('success', 'Offer created successfully. Redirecting to Browse Offers...');
      form.reset();
      refreshSelectedBookInfo();
      refreshImagePreview();

      setTimeout(() => {
        window.location.href = '/offers-browse';
      }, 900);
    } catch (error) {
      setStatus('error', error.message || 'Something went wrong while creating your offer.');
    } finally {
      setSubmitting(false);
    }
  });

  resetBtn.addEventListener('click', () => {
    form.reset();
    refreshSelectedBookInfo();
    refreshImagePreview();
    clearStatus();
  });

  bookSelect.addEventListener('change', refreshSelectedBookInfo);
  imagesInput.addEventListener('change', refreshImagePreview);

  loadBooks();
});
