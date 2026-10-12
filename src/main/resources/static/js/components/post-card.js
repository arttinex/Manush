/**
 * post-card.js — Web component or helper for rendering HelpPost cards.
 */
export function renderPostCard(post) {
  if (!post) return '';

  const urgency = post.urgency || 'NORMAL';
  const urgencyClass = {
    'EMERGENCY': 'badge-emergency',
    'HIGH': 'badge-high',
    'NORMAL': 'badge-normal',
    'LOW': 'badge-low'
  }[urgency] || 'badge-normal';

  const authorName = post.authorName || 'Anonymous';
  let timeFormatted = 'Recently';
  if (post.createdAt) {
    try {
      const d = new Date(post.createdAt);
      if (!isNaN(d.getTime())) {
        timeFormatted = d.toLocaleDateString(undefined, {
          month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
      }
    } catch {}
  }

  let locationText = '';
  if (post.location) {
    if (typeof post.location === 'string') {
      locationText = '· ' + post.location;
    } else if (post.location.area) {
      locationText = '· ' + post.location.area;
    } else if (post.location.city) {
      locationText = '· ' + post.location.city;
    }
  }

  const categoryLabel = post.category ? String(post.category).replace(/_/g, ' ') : '';

  const commentsList = Array.isArray(post.comments) ? post.comments : [];
  const commentsHtml = commentsList.map(comment => {
    const cAuthor = comment.authorName || 'Anonymous';
    let cTime = '';
    if (comment.createdAt) {
      try {
        const d = new Date(comment.createdAt);
        if (!isNaN(d.getTime())) {
          cTime = d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        }
      } catch {}
    }
    return `
      <div class="comment-item" data-comment-id="${comment.id || ''}">
        <div class="comment-header">
          <div class="comment-avatar">${cAuthor.charAt(0).toUpperCase()}</div>
          <div class="comment-meta">
            <span class="comment-author">${cAuthor}</span>
            <span class="comment-time">${cTime}</span>
          </div>
        </div>
        <div class="comment-body">${comment.content || comment.text || ''}</div>
      </div>
    `;
  }).join('');

  return `
    <article class="card-3d post-card" data-id="${post.id || ''}">
      <div class="post-card-header">
        <div class="avatar" style="display:flex;align-items:center;justify-content:center;font-weight:bold;color:var(--manush-primary)">
          ${authorName.charAt(0).toUpperCase()}
        </div>
        <div class="post-meta">
          <div class="post-author">${authorName}</div>
          <div class="post-time">${timeFormatted} ${locationText}</div>
        </div>
        <span class="badge-urgency ${urgencyClass}">${urgency}</span>
        <span class="post-category-tag">${categoryLabel}</span>
      </div>
      <h3 class="post-title">${post.title || ''}</h3>
      <p class="post-body">${post.description || ''}</p>
      <div class="post-actions">
        <button class="post-action-btn btn-react" data-id="${post.id || ''}">
          ❤️ <span>${post.reactionCount || 0}</span>
        </button>
        <button class="post-action-btn btn-comment-toggle" data-id="${post.id || ''}">
          💬 <span>${post.commentCount || 0} Comments</span>
        </button>
        <button class="post-action-btn btn-share" data-id="${post.id || ''}">
          🔗 Share
        </button>
        <button class="post-action-btn btn-report" data-id="${post.id || ''}" style="margin-left:auto">
          🚩 Report
        </button>
      </div>
      <div class="comments-section" id="comments-${post.id || ''}" style="display:none">
        <div class="comments-list">${commentsHtml}</div>
        <form class="comment-form" data-post-id="${post.id || ''}">
          <input type="text" class="form-input comment-input" placeholder="Write a comment..." required>
          <button type="submit" class="btn btn-3d btn-sm">Post</button>
        </form>
      </div>
    </article>
  `;
}
