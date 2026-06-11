const WRONG_BOOK_COUNT_KEY = "elearning_wrong_book_count";
const WRONG_BOOK_EVENT = "elearning-wrong-book-updated";

function normalizeWrongBookCount(value) {
  const count = Number(value);
  if (!Number.isFinite(count) || count < 0) {
    return 0;
  }
  return Math.floor(count);
}

export function getWrongBookCount() {
  return normalizeWrongBookCount(localStorage.getItem(WRONG_BOOK_COUNT_KEY));
}

export function applyWrongBookCount(value) {
  const count = normalizeWrongBookCount(value);
  localStorage.setItem(WRONG_BOOK_COUNT_KEY, String(count));
  window.dispatchEvent(
    new CustomEvent(WRONG_BOOK_EVENT, {
      detail: { wrongBookCount: count }
    })
  );
  return count;
}

export function applyWrongBookCountFromPayload(payload) {
  if (!payload || payload.wrongBookCount === undefined || payload.wrongBookCount === null) {
    return null;
  }
  return applyWrongBookCount(payload.wrongBookCount);
}

export function onWrongBookCountChange(handler) {
  const listener = (event) => {
    handler(normalizeWrongBookCount(event?.detail?.wrongBookCount));
  };
  window.addEventListener(WRONG_BOOK_EVENT, listener);
  return () => window.removeEventListener(WRONG_BOOK_EVENT, listener);
}
