(() => {
  const state = {
    books: [],
    filteredBooks: [],
    offers: [],
    exchangeRequests: [],
    users: []
  };

  const panelKeys = ["dashboard", "books", "offers", "exchangeRequests", "users"];

  const elements = {
    sidebar: document.getElementById("sidebar"),
    menuToggle: document.getElementById("menuToggle"),
    navLinks: Array.from(document.querySelectorAll(".nav-link")),
    panels: Object.fromEntries(panelKeys.map((key) => [key, document.getElementById(`panel-${key}`)])),

    statBooks: document.getElementById("statBooks"),
    statOffers: document.getElementById("statOffers"),
    statExchangeRequests: document.getElementById("statExchangeRequests"),
    statUsers: document.getElementById("statUsers"),
    dashboardSummaryBody: document.getElementById("dashboardSummaryBody"),

    booksTableBody: document.getElementById("booksTableBody"),
    offersTableBody: document.getElementById("offersTableBody"),
    exchangeRequestsTableBody: document.getElementById("exchangeRequestsTableBody"),
    usersTableBody: document.getElementById("usersTableBody"),

    booksEmpty: document.getElementById("booksEmpty"),
    offersEmpty: document.getElementById("offersEmpty"),
    exchangeRequestsEmpty: document.getElementById("exchangeRequestsEmpty"),
    usersEmpty: document.getElementById("usersEmpty"),

    booksSearchInput: document.getElementById("booksSearchInput"),
    resetBooksSearchBtn: document.getElementById("resetBooksSearchBtn"),

    addBookBtn: document.getElementById("addBookBtn"),
    refreshDashboard: document.getElementById("refreshDashboard"),
    refreshOffers: document.getElementById("refreshOffers"),
    refreshExchangeRequests: document.getElementById("refreshExchangeRequests"),
    refreshUsers: document.getElementById("refreshUsers"),

    bookModal: document.getElementById("bookModal"),
    bookModalTitle: document.getElementById("bookModalTitle"),
    bookForm: document.getElementById("bookForm"),
    cancelBookModal: document.getElementById("cancelBookModal"),
    bookId: document.getElementById("bookId"),
    bookTitle: document.getElementById("bookTitle"),
    bookAuthor: document.getElementById("bookAuthor"),
    bookYear: document.getElementById("bookYear"),
    bookIsbn: document.getElementById("bookIsbn"),
    bookCoverImg: document.getElementById("bookCoverImg"),
    bookDescription: document.getElementById("bookDescription"),

    toast: document.getElementById("toast")
  };

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/\"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function formatDate(value) {
    if (!value) {
      return "-";
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
  }

  function setActivePanel(section) {
    elements.navLinks.forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.section === section);
    });

    panelKeys.forEach((key) => {
      elements.panels[key].classList.toggle("is-visible", key === section);
    });

    elements.sidebar.classList.remove("open");
  }

  async function request(url, options = {}) {
    const response = await fetch(url, {
      method: options.method || "GET",
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {})
      },
      body: options.body ? JSON.stringify(options.body) : undefined
    });

    const text = await response.text();
    const payload = text ? (() => {
      try {
        return JSON.parse(text);
      } catch (_ignored) {
        return text;
      }
    })() : null;

    if (!response.ok) {
      const message = payload?.message || payload?.error || `Request failed with status ${response.status}`;
      throw new Error(message);
    }

    // Admin APIs now use a standard envelope: { success, message, data }.
    if (payload && typeof payload === "object" && Object.prototype.hasOwnProperty.call(payload, "success")
      && Object.prototype.hasOwnProperty.call(payload, "data")) {
      if (!payload.success) {
        throw new Error(payload.message || "Request failed.");
      }
      return payload.data;
    }

    return payload;
  }

  function showToast(message, isError = false) {
    elements.toast.textContent = message;
    elements.toast.style.background = isError ? "#8f1f2f" : "#14243e";
    elements.toast.classList.add("show");
    window.setTimeout(() => elements.toast.classList.remove("show"), 2400);
  }

  function renderDashboard() {
    elements.statBooks.textContent = String(state.books.length);
    elements.statOffers.textContent = String(state.offers.length);
    elements.statExchangeRequests.textContent = String(state.exchangeRequests.length);
    elements.statUsers.textContent = String(state.users.length);

    const refreshed = new Date().toLocaleString();
    const rows = [
      ["Books", state.books.length],
      ["Offers", state.offers.length],
      ["Exchange Requests", state.exchangeRequests.length],
      ["Users", state.users.length]
    ];

    elements.dashboardSummaryBody.innerHTML = rows
      .map(([label, count]) => `<tr><td>${label}</td><td>${count}</td><td>${refreshed}</td></tr>`)
      .join("");
  }

  function renderBooks() {
    if (!state.filteredBooks.length) {
      elements.booksTableBody.innerHTML = "";
      elements.booksEmpty.hidden = false;
      return;
    }

    elements.booksEmpty.hidden = true;
    elements.booksTableBody.innerHTML = state.filteredBooks
      .map((book) => `
        <tr>
          <td>${escapeHtml(book.title)}</td>
          <td>${escapeHtml(book.author)}</td>
          <td>${escapeHtml(book.publicationYear ?? "-")}</td>
          <td>
            <button class="btn" data-edit-book="${escapeHtml(book.id)}">Edit</button>
            <button class="btn danger" data-delete-book="${escapeHtml(book.id)}">Delete</button>
          </td>
        </tr>
      `)
      .join("");
  }

  function applyBooksFilter() {
    const term = (elements.booksSearchInput?.value || "").trim().toLowerCase();
    if (!term) {
      state.filteredBooks = [...state.books];
      renderBooks();
      return;
    }

    state.filteredBooks = state.books.filter((book) => {
      const title = String(book.title || "").toLowerCase();
      const author = String(book.author || "").toLowerCase();
      return title.includes(term) || author.includes(term);
    });
    renderBooks();
  }

  function renderOffers() {
    if (!state.offers.length) {
      elements.offersTableBody.innerHTML = "";
      elements.offersEmpty.hidden = false;
      return;
    }

    elements.offersEmpty.hidden = true;
    elements.offersTableBody.innerHTML = state.offers
      .map((offer) => `
        <tr>
          <td>${escapeHtml(offer.bookTitle)}</td>
          <td>${escapeHtml(offer.ownerName)}</td>
          <td>${escapeHtml(offer.condition)}</td>
          <td><span class="badge ${escapeHtml(offer.status)}">${escapeHtml(offer.status)}</span></td>
          <td>
            <button class="btn warn" data-block-offer="${escapeHtml(offer.offerId)}">Block</button>
            <button class="btn danger" data-delete-offer="${escapeHtml(offer.offerId)}">Delete</button>
          </td>
        </tr>
      `)
      .join("");
  }

  function renderExchangeRequests() {
    if (!state.exchangeRequests.length) {
      elements.exchangeRequestsTableBody.innerHTML = "";
      elements.exchangeRequestsEmpty.hidden = false;
      return;
    }

    elements.exchangeRequestsEmpty.hidden = true;
    elements.exchangeRequestsTableBody.innerHTML = state.exchangeRequests
      .map((item) => `
        <tr>
          <td>${escapeHtml(item.requesterUser)}</td>
          <td>${escapeHtml(item.targetBookTitle)}</td>
          <td><span class="badge ${escapeHtml(item.status)}">${escapeHtml(item.status)}</span></td>
          <td>${escapeHtml(formatDate(item.createdAt))}</td>
        </tr>
      `)
      .join("");
  }

  function renderUsers() {
    if (!state.users.length) {
      elements.usersTableBody.innerHTML = "";
      elements.usersEmpty.hidden = false;
      return;
    }

    elements.usersEmpty.hidden = true;
    elements.usersTableBody.innerHTML = state.users
      .map((user) => `
        <tr>
          <td>${escapeHtml(user.name)}</td>
          <td>${escapeHtml(user.email)}</td>
          <td>${escapeHtml(user.role || "-")}</td>
          <td><span class="badge ${escapeHtml(user.status)}">${escapeHtml(user.status)}</span></td>
          <td>
            ${user.status === "DEACTIVATED"
              ? "<span class=\"badge DEACTIVATED\">Blocked</span>"
              : `<button class=\"btn warn\" data-block-user=\"${escapeHtml(user.id)}\">Block</button>`}
          </td>
        </tr>
      `)
      .join("");
  }

  async function loadAllData() {
    const [books, offers, exchangeRequests, users] = await Promise.all([
      request("/admin/books"),
      request("/admin/offers"),
      request("/admin/exchange-requests"),
      request("/admin/users")
    ]);

    state.books = Array.isArray(books) ? books : [];
    state.filteredBooks = [...state.books];
    state.offers = Array.isArray(offers) ? offers : [];
    state.exchangeRequests = Array.isArray(exchangeRequests) ? exchangeRequests : [];
    state.users = Array.isArray(users) ? users : [];

    renderDashboard();
    renderBooks();
    renderOffers();
    renderExchangeRequests();
    renderUsers();
  }

  function openBookModal(book) {
    elements.bookModal.classList.add("is-open");
    elements.bookModal.setAttribute("aria-hidden", "false");
    if (!book) {
      elements.bookModalTitle.textContent = "Add Book";
      elements.bookId.value = "";
      elements.bookTitle.value = "";
      elements.bookAuthor.value = "";
      elements.bookYear.value = "";
      elements.bookIsbn.value = "";
      elements.bookCoverImg.value = "";
      elements.bookDescription.value = "";
      return;
    }

    elements.bookModalTitle.textContent = "Edit Book";
    elements.bookId.value = book.id || "";
    elements.bookTitle.value = book.title || "";
    elements.bookAuthor.value = book.author || "";
    elements.bookYear.value = book.publicationYear || "";
    elements.bookIsbn.value = book.isbn || "";
    elements.bookCoverImg.value = book.coverImg || "";
    elements.bookDescription.value = book.description || "";
  }

  function closeBookModal() {
    elements.bookModal.classList.remove("is-open");
    elements.bookModal.setAttribute("aria-hidden", "true");
  }

  async function saveBook(event) {
    event.preventDefault();

    const id = elements.bookId.value.trim();
    const payload = {
      title: elements.bookTitle.value.trim(),
      author: elements.bookAuthor.value.trim(),
      publicationYear: elements.bookYear.value ? Number(elements.bookYear.value) : null,
      isbn: elements.bookIsbn.value.trim() || null,
      coverImg: elements.bookCoverImg.value.trim() || null,
      description: elements.bookDescription.value.trim() || null
    };

    if (!payload.title || !payload.author) {
      showToast("Title and author are required.", true);
      return;
    }

    if (id) {
      await request(`/admin/books/${encodeURIComponent(id)}`, { method: "PUT", body: payload });
      showToast("Book updated.");
    } else {
      await request("/admin/books", { method: "POST", body: payload });
      showToast("Book created.");
    }

    closeBookModal();
    state.books = await request("/admin/books");
    applyBooksFilter();
    renderDashboard();
  }

  async function deleteBook(id) {
    if (!window.confirm("Delete this book?")) {
      return;
    }

    await request(`/admin/books/${encodeURIComponent(id)}`, { method: "DELETE" });
    showToast("Book deleted.");
    state.books = await request("/admin/books");
    applyBooksFilter();
    renderDashboard();
  }

  async function blockOffer(id) {
    await request(`/admin/offers/${id}/block`, { method: "PUT" });
    showToast("Offer blocked.");
    state.offers = await request("/admin/offers");
    renderOffers();
    renderDashboard();
  }

  async function deleteOffer(id) {
    if (!window.confirm("Delete this offer?")) {
      return;
    }

    await request(`/admin/offers/${id}`, { method: "DELETE" });
    showToast("Offer deleted.");
    state.offers = await request("/admin/offers");
    renderOffers();
    renderDashboard();
  }

  async function blockUser(id) {
    if (!window.confirm("Block this user account?")) {
      return;
    }

    await request(`/admin/users/${id}/block`, { method: "PUT" });
    showToast("User blocked.");
    state.users = await request("/admin/users");
    renderUsers();
    renderDashboard();
  }

  function attachEvents() {
    elements.menuToggle.addEventListener("click", () => {
      elements.sidebar.classList.toggle("open");
    });

    elements.navLinks.forEach((btn) => {
      btn.addEventListener("click", () => {
        setActivePanel(btn.dataset.section);
      });
    });

    elements.addBookBtn.addEventListener("click", () => openBookModal(null));
    if (elements.booksSearchInput) {
      elements.booksSearchInput.addEventListener("input", applyBooksFilter);
    }
    if (elements.resetBooksSearchBtn) {
      elements.resetBooksSearchBtn.addEventListener("click", () => {
        if (!elements.booksSearchInput) {
          return;
        }
        elements.booksSearchInput.value = "";
        applyBooksFilter();
      });
    }
    elements.cancelBookModal.addEventListener("click", closeBookModal);
    elements.bookModal.addEventListener("click", (event) => {
      if (event.target === elements.bookModal) {
        closeBookModal();
      }
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && elements.bookModal.classList.contains("is-open")) {
        closeBookModal();
      }
    });
    elements.bookForm.addEventListener("submit", async (event) => {
      try {
        await saveBook(event);
      } catch (error) {
        showToast(error.message || "Failed to save book.", true);
      }
    });

    elements.refreshDashboard.addEventListener("click", async () => {
      try {
        await loadAllData();
        showToast("Dashboard refreshed.");
      } catch (error) {
        showToast(error.message || "Refresh failed.", true);
      }
    });

    elements.refreshOffers.addEventListener("click", async () => {
      try {
        state.offers = await request("/admin/offers");
        renderOffers();
        renderDashboard();
      } catch (error) {
        showToast(error.message || "Failed to refresh offers.", true);
      }
    });

    elements.refreshExchangeRequests.addEventListener("click", async () => {
      try {
        state.exchangeRequests = await request("/admin/exchange-requests");
        renderExchangeRequests();
        renderDashboard();
      } catch (error) {
        showToast(error.message || "Failed to refresh exchange requests.", true);
      }
    });

    elements.refreshUsers.addEventListener("click", async () => {
      try {
        state.users = await request("/admin/users");
        renderUsers();
        renderDashboard();
      } catch (error) {
        showToast(error.message || "Failed to refresh users.", true);
      }
    });

    elements.booksTableBody.addEventListener("click", async (event) => {
      const editBtn = event.target.closest("[data-edit-book]");
      if (editBtn) {
        const book = state.filteredBooks.find((item) => String(item.id) === editBtn.getAttribute("data-edit-book"));
        openBookModal(book || null);
        return;
      }

      const deleteBtn = event.target.closest("[data-delete-book]");
      if (deleteBtn) {
        try {
          await deleteBook(deleteBtn.getAttribute("data-delete-book"));
        } catch (error) {
          showToast(error.message || "Failed to delete book.", true);
        }
      }
    });

    elements.offersTableBody.addEventListener("click", async (event) => {
      const blockBtn = event.target.closest("[data-block-offer]");
      if (blockBtn) {
        try {
          await blockOffer(Number(blockBtn.getAttribute("data-block-offer")));
        } catch (error) {
          showToast(error.message || "Failed to block offer.", true);
        }
        return;
      }

      const deleteBtn = event.target.closest("[data-delete-offer]");
      if (deleteBtn) {
        try {
          await deleteOffer(Number(deleteBtn.getAttribute("data-delete-offer")));
        } catch (error) {
          showToast(error.message || "Failed to delete offer.", true);
        }
      }
    });

    elements.usersTableBody.addEventListener("click", async (event) => {
      const blockBtn = event.target.closest("[data-block-user]");
      if (!blockBtn) {
        return;
      }

      try {
        await blockUser(Number(blockBtn.getAttribute("data-block-user")));
      } catch (error) {
        showToast(error.message || "Failed to block user.", true);
      }
    });
  }

  (async () => {
    attachEvents();
    closeBookModal();
    try {
      await loadAllData();
    } catch (error) {
      showToast(error.message || "Failed to load admin data.", true);
    }
  })();
})();
