(function () {
	'use strict';

	var cfg = window.IH_DUP_CONFIG || {};
	var resolvedBy = cfg.resolvedBy || '';

	/**
	 * CSRFGuard blocks *.form POSTs; WS REST proxy lives under /ws/rest/v1/... Normalise legacy
	 * cached pages that still set proxyBase to .../module/ihmodule.
	 */
	function normalizePatientExchangeProxyBase(raw) {
		var b = (raw || '').replace(/\/$/, '');
		if (b && /\/module\/ihmodule$/.test(b)) {
			return b.replace(/\/module\/ihmodule$/, '') + '/ws/rest/v1/ihmodule/patient-exchange';
		}
		return b;
	}

	var proxyBase = normalizePatientExchangeProxyBase(cfg.proxyBase || '');

	var CAND_TABLE_COLSPAN = 11;

	/** Case UUID for the open duplicate-review modal (candidate actions). */
	var dupReviewModalCaseUuid = '';
	var dupReviewModalSourceUuid = '';

	function normalizeCandidatesPayload(raw) {
		if (!raw || typeof raw !== 'object') {
			return {
				openmrsCandidates: [],
				fhirCandidates: [],
				totalStoredCount: 0,
				visibleCount: 0,
				hiddenCount: 0
			};
		}
		if (Array.isArray(raw)) {
			return {
				openmrsCandidates: [],
				fhirCandidates: raw,
				totalStoredCount: raw.length,
				visibleCount: raw.length,
				hiddenCount: 0
			};
		}
		var openmrs = Array.isArray(raw.openmrsCandidates) ? raw.openmrsCandidates : [];
		var fhir = Array.isArray(raw.fhirCandidates) ? raw.fhirCandidates : [];
		var visible = raw.visibleCount != null ? Number(raw.visibleCount) : openmrs.length + fhir.length;
		return {
			openmrsCandidates: openmrs,
			fhirCandidates: fhir,
			totalStoredCount: raw.totalStoredCount != null ? Number(raw.totalStoredCount) : visible,
			visibleCount: visible,
			hiddenCount: raw.hiddenCount != null ? Number(raw.hiddenCount) : 0
		};
	}

	function fetchDupReviewGet(url) {
		return fetch(url, { credentials: 'same-origin', cache: 'no-store' });
	}

	function updateModalCandidateCount(meta) {
		var el = document.getElementById('modalSrcCandCount');
		if (!el || !meta) {
			return;
		}
		if (meta.visibleCount > 0) {
			el.textContent = String(meta.visibleCount);
			return;
		}
		if (meta.hiddenCount > 0 && meta.totalStoredCount > 0) {
			el.textContent =
				'0 to review (' + meta.hiddenCount + ' of ' + meta.totalStoredCount + ' removed)';
			return;
		}
		el.textContent = '0';
	}

	function emptyMessageForCandidateTab(list, tabFiltered) {
		if (list && list.length) {
			return null;
		}
		if (tabFiltered) {
			return 'No candidates match the selected match type.';
		}
		if (dupCandMeta.hiddenCount > 0 && dupCandMeta.visibleCount === 0) {
			return (
				'All ' +
				dupCandMeta.hiddenCount +
				' stored candidate(s) were removed from this list.'
			);
		}
		return 'No candidates in this category.';
	}

	function formatMatchScoreCell(score) {
		if (score == null || score === '') {
			return '—';
		}
		var n = Number(score);
		if (isNaN(n)) {
			return escapeHtml(String(score));
		}
		return escapeHtml(n.toFixed(4));
	}

	function setDupTabBadges(nOpenmrs, nFhir) {
		var a = document.getElementById('dupTabBadgeOpenmrs');
		var b = document.getElementById('dupTabBadgeFhir');
		if (a) {
			a.textContent = String(nOpenmrs != null ? nOpenmrs : 0);
		}
		if (b) {
			b.textContent = String(nFhir != null ? nFhir : 0);
		}
	}

	function activateDupCandidateTab(which) {
		var $ = window.jQuery;
		var tabOpen = document.getElementById('dupTabOpenmrs');
		var tabFhir = document.getElementById('dupTabFhir');
		var paneOpen = document.getElementById('dupPaneOpenmrs');
		var paneFhir = document.getElementById('dupPaneFhir');
		if ($ && tabOpen && tabFhir && typeof $.fn.tab === 'function') {
			if (which === 'fhir') {
				$(tabFhir).tab('show');
			} else {
				$(tabOpen).tab('show');
			}
			return;
		}
		if (tabOpen && tabFhir && paneOpen && paneFhir) {
			if (which === 'fhir') {
				tabOpen.classList.remove('active');
				tabFhir.classList.add('active');
				paneOpen.classList.remove('show', 'active');
				paneFhir.classList.add('show', 'active');
				tabOpen.setAttribute('aria-selected', 'false');
				tabFhir.setAttribute('aria-selected', 'true');
			} else {
				tabFhir.classList.remove('active');
				tabOpen.classList.add('active');
				paneFhir.classList.remove('show', 'active');
				paneOpen.classList.add('show', 'active');
				tabFhir.setAttribute('aria-selected', 'false');
				tabOpen.setAttribute('aria-selected', 'true');
			}
		}
	}

	function renderCandidatesTable(tbody, list, sourcePatientUuid, customEmptyMessage) {
		if (!tbody) {
			return;
		}
		tbody.innerHTML = '';
		if (!list || !list.length) {
			var msg = customEmptyMessage != null ? customEmptyMessage : 'No candidates in this category.';
			tbody.innerHTML =
				'<tr><td colspan="' +
				CAND_TABLE_COLSPAN +
				'" class="text-muted">' +
				escapeHtml(msg) +
				'</td></tr>';
			return;
		}
		list.forEach(function (c) {
			var nm = [c.candidateGiven || '', c.candidateFamily || ''].join(' ').trim() || '—';
			var tr = document.createElement('tr');
			tr.innerHTML =
				'<td><code>' +
				escapeHtml(c.fhirPatientLogicalId || '') +
				'</code></td>' +
				'<td><span class="badge bg-light text-dark border">' +
				escapeHtml(c.mpiIdentifierValue || '—') +
				'</span></td>' +
				'<td>' +
				escapeHtml(nm) +
				'</td>' +
				'<td>' +
				escapeHtml(c.candidateBirthdate || '') +
				'</td>' +
				'<td>' +
				escapeHtml(c.candidateGenderCode || '') +
				'</td>' +
				'<td>' +
				escapeHtml(c.candidateTelecom || '') +
				'</td>' +
				'<td class="small text-break" style="white-space:pre-line;max-width:280px">' +
				escapeHtml(c.candidateAddressSnapshot || '') +
				'</td>' +
				'<td>' +
				formatMatchScoreCell(c.matchScore) +
				'</td>' +
				'<td>' +
				(c.matchType ? escapeHtml(c.matchType) : '—') +
				'</td>' +
				'<td>' +
				(c.sourceOfPatient ? escapeHtml(c.sourceOfPatient) : '—') +
				'</td>' +
				'<td class="text-nowrap">' +
				'<button type="button" class="btn btn-sm btn-success btn-add-patient-cand me-1">Link And Join Patient</button>' +
				'<button type="button" class="btn btn-sm btn-outline-secondary btn-skip-dup-candidate">Remove</button>' +
				'</td>';
			tr.dataset.fhirLogicalId = c.fhirPatientLogicalId || '';
			tr.dataset.mpiIdentifierValue = c.mpiIdentifierValue || '';
			tr.dataset.candidateId = c.id != null ? String(c.id) : '';
			tbody.appendChild(tr);
		});

		Array.prototype.forEach.call(tbody.querySelectorAll('.btn-add-patient-cand'), function (btn) {
			btn.addEventListener('click', function () {
				var tr = btn.closest('tr');
				var cid = (tr.dataset.candidateId || '').trim();
				postAddPatientCandidate(dupReviewModalCaseUuid, cid, btn);
			});
		});
		Array.prototype.forEach.call(tbody.querySelectorAll('.btn-skip-dup-candidate'), function (btn) {
			btn.addEventListener('click', function () {
				var tr = btn.closest('tr');
				var cid = (tr.dataset.candidateId || '').trim();
				postSkipDuplicateReviewCandidate(dupReviewModalCaseUuid, cid, btn);
			});
		});
	}

	var dupCandRawOpenmrs = [];
	var dupCandRawFhir = [];
	var dupCandSourceUuid = '';
	var dupCandMeta = { totalStoredCount: 0, visibleCount: 0, hiddenCount: 0 };
	var dupMatchTypeFilterWired = false;

	function uniqueSortedMatchTypesFromList(list) {
		var seen = Object.create(null);
		var types = [];
		var hasBlank = false;
		if (!list || !list.length) {
			return { types: types, hasBlank: false };
		}
		list.forEach(function (c) {
			var raw = c && c.matchType != null ? String(c.matchType) : '';
			var t = raw.trim();
			if (!t) {
				hasBlank = true;
				return;
			}
			if (!seen[t]) {
				seen[t] = true;
				types.push(t);
			}
		});
		types.sort(function (a, b) {
			return a.localeCompare(b);
		});
		return { types: types, hasBlank: hasBlank };
	}

	function rebuildDupMatchTypeSelect(selectEl, list) {
		if (!selectEl) {
			return;
		}
		var u = uniqueSortedMatchTypesFromList(list);
		selectEl.innerHTML = '';
		var optAll = document.createElement('option');
		optAll.value = '';
		optAll.textContent = 'All match types';
		selectEl.appendChild(optAll);
		if (u.hasBlank) {
			var optBlank = document.createElement('option');
			optBlank.value = '__EMPTY__';
			optBlank.textContent = '(No match type)';
			selectEl.appendChild(optBlank);
		}
		for (var i = 0; i < u.types.length; i++) {
			var t = u.types[i];
			var o = document.createElement('option');
			o.value = t;
			o.textContent = t;
			selectEl.appendChild(o);
		}
		selectEl.value = '';
	}

	function wireDupMatchTypeFiltersOnce() {
		if (dupMatchTypeFilterWired) {
			return;
		}
		dupMatchTypeFilterWired = true;
		var selOm = document.getElementById('dupMatchTypeFilterOpenmrs');
		var selFh = document.getElementById('dupMatchTypeFilterFhir');
		if (selOm) {
			selOm.addEventListener('change', function () {
				applyDupMatchTypeFilter('openmrs');
			});
		}
		if (selFh) {
			selFh.addEventListener('change', function () {
				applyDupMatchTypeFilter('fhir');
			});
		}
	}

	function applyDupMatchTypeFilter(tabKey) {
		var raw = tabKey === 'openmrs' ? dupCandRawOpenmrs : dupCandRawFhir;
		var sel = document.getElementById(
			tabKey === 'openmrs' ? 'dupMatchTypeFilterOpenmrs' : 'dupMatchTypeFilterFhir'
		);
		var tbody =
			tabKey === 'openmrs'
				? document.getElementById('candidatesBodyOpenmrs')
				: document.getElementById('candidatesBodyFhir');
		if (!sel || !tbody) {
			return;
		}
		var v = sel.value;
		var filtered = raw;
		if (v === '__EMPTY__') {
			filtered = raw.filter(function (c) {
				return !c || c.matchType == null || !String(c.matchType).trim();
			});
		} else if (v) {
			filtered = raw.filter(function (c) {
				return c && String(c.matchType || '').trim() === v;
			});
		}
		var emptyMsg = emptyMessageForCandidateTab(filtered, Boolean(v));
		renderCandidatesTable(tbody, filtered, dupCandSourceUuid, emptyMsg);
	}

	function setupDupTabCandidatesWithMatchTypeFilter(tabKey, list, sourcePatientUuid) {
		var sel = document.getElementById(
			tabKey === 'openmrs' ? 'dupMatchTypeFilterOpenmrs' : 'dupMatchTypeFilterFhir'
		);
		var tbody =
			tabKey === 'openmrs'
				? document.getElementById('candidatesBodyOpenmrs')
				: document.getElementById('candidatesBodyFhir');
		rebuildDupMatchTypeSelect(sel, list);
		renderCandidatesTable(tbody, list, sourcePatientUuid, null);
	}

	/** Full pending list from last successful GET (client-side pagination). */
	var pendingRowsCache = [];
	var pendingCurrentPage = 1;
	var pendingPaginationWired = false;

	function getPendingPageSize() {
		var sel = document.getElementById('pendingPageSize');
		if (!sel) {
			return 10;
		}
		var v = parseInt(sel.value, 10);
		if (isNaN(v) || v < 0) {
			return 10;
		}
		return v;
	}

	function totalPendingPages(nRows, pageSize) {
		if (nRows === 0) {
			return 1;
		}
		if (pageSize === 0) {
			return 1;
		}
		return Math.max(1, Math.ceil(nRows / pageSize));
	}

	function buildPendingRowElement(row) {
		var tr = document.createElement('tr');
		var name = [row.sourceGiven || '', row.sourceFamily || ''].join(' ').trim() || '—';
		tr.innerHTML =
			'<td>' +
			escapeHtml(row.dateCreated || '') +
			'</td>' +
			'<td><code>' +
			escapeHtml(row.localPatientUuid || '') +
			'</code></td>' +
			'<td>' +
			escapeHtml(name) +
			'</td>' +
			'<td>' +
			escapeHtml(row.sourceBirthdate || '') +
			'</td>' +
			'<td>' +
			escapeHtml(row.sourceGenderCode || '') +
			'</td>' +
			'<td class="small text-break" style="white-space:pre-line;max-width:280px">' +
			escapeHtml(row.candidateAddressSnapshot || '') +
			'</td>' +
			'<td>' +
			String(row.candidateCount != null ? row.candidateCount : '') +
			'</td>' +
			'<td><small>' +
			escapeHtml(row.caseUuid || '') +
			'</small></td>' +
			'<td>' +
			'<button type="button" class="btn btn-sm btn-outline-primary me-1 btn-view-dup">View duplicates</button>' +
			'<button type="button" class="btn btn-sm btn-primary me-1 btn-add-patient-pending">Register New Patient</button>' +
			'<button type="button" class="btn btn-sm btn-outline-secondary btn-skip-pending">Remove</button>' +
			'</td>';
		tr.dataset.caseUuid = row.caseUuid || '';
		tr.dataset.localPatientUuid = row.localPatientUuid || '';
		tr.dataset.sourceTelecom = row.sourceTelecom || '';
		return tr;
	}

	function bindPendingRowButtons(tbody) {
		Array.prototype.forEach.call(tbody.querySelectorAll('.btn-view-dup'), function (btn) {
			btn.addEventListener('click', function () {
				var tr = btn.closest('tr');
				openCandidatesModal(
					tr.dataset.caseUuid,
					tr.dataset.localPatientUuid,
					readSourceMetaFromPendingRow(tr)
				);
			});
		});
		Array.prototype.forEach.call(tbody.querySelectorAll('.btn-add-patient-pending'), function (btn) {
			btn.addEventListener('click', function () {
				var tr = btn.closest('tr');
				doAddPatientFromPending(tr.dataset.localPatientUuid, tr.dataset.caseUuid, btn);
			});
		});
		Array.prototype.forEach.call(tbody.querySelectorAll('.btn-skip-pending'), function (btn) {
			btn.addEventListener('click', function () {
				var tr = btn.closest('tr');
				doSkipPendingCase(tr.dataset.caseUuid, btn);
			});
		});
	}

	function updatePendingPaginationControls(startIdx, endIdx, totalRows, pageCount, pageSize) {
		var info = document.getElementById('pendingPaginationInfo');
		var ind = document.getElementById('pendingPageIndicator');
		var prevBtn = document.getElementById('pendingPagePrev');
		var nextBtn = document.getElementById('pendingPageNext');
		var prevLi = document.getElementById('pendingPagePrevLi');
		var nextLi = document.getElementById('pendingPageNextLi');
		if (info) {
			if (pageSize === 0) {
				info.textContent = 'Showing all ' + totalRows + ' row(s)';
			} else {
				info.textContent =
					'Showing ' + startIdx + '–' + endIdx + ' of ' + totalRows + ' row(s)';
			}
		}
		if (ind) {
			ind.textContent = 'Page ' + pendingCurrentPage + ' / ' + pageCount;
		}
		var prevDis = pendingCurrentPage <= 1 || pageSize === 0 || totalRows === 0;
		var nextDis = pendingCurrentPage >= pageCount || pageSize === 0 || totalRows === 0;
		if (prevBtn) {
			prevBtn.disabled = prevDis;
		}
		if (nextBtn) {
			nextBtn.disabled = nextDis;
		}
		if (prevLi) {
			prevLi.classList.toggle('disabled', prevDis);
		}
		if (nextLi) {
			nextLi.classList.toggle('disabled', nextDis);
		}
	}

	function renderPendingTablePage() {
		var tbody = document.getElementById('pendingCasesBody');
		var wrap = document.getElementById('pendingPaginationWrap');
		if (!tbody) {
			return;
		}
		var n = pendingRowsCache.length;
		if (n === 0) {
			tbody.innerHTML =
				'<tr><td colspan="9" class="text-center text-muted">No pending duplicate-review cases.</td></tr>';
			if (wrap) {
				wrap.style.display = 'none';
			}
			return;
		}
		var ps = getPendingPageSize();
		var pageCount = totalPendingPages(n, ps);
		if (pendingCurrentPage > pageCount) {
			pendingCurrentPage = pageCount;
		}
		if (pendingCurrentPage < 1) {
			pendingCurrentPage = 1;
		}
		var start = ps === 0 ? 0 : (pendingCurrentPage - 1) * ps;
		var slice =
			ps === 0 ? pendingRowsCache.slice() : pendingRowsCache.slice(start, start + ps);
		var startDisplay = ps === 0 ? 1 : start + 1;
		var endDisplay = ps === 0 ? n : start + slice.length;

		tbody.innerHTML = '';
		slice.forEach(function (row) {
			tbody.appendChild(buildPendingRowElement(row));
		});
		bindPendingRowButtons(tbody);

		if (wrap) {
			wrap.style.display = '';
		}
		updatePendingPaginationControls(startDisplay, endDisplay, n, pageCount, ps);
	}

	function wirePendingPaginationIfNeeded() {
		if (pendingPaginationWired) {
			return;
		}
		pendingPaginationWired = true;
		var prevBtn = document.getElementById('pendingPagePrev');
		var nextBtn = document.getElementById('pendingPageNext');
		var sizeSel = document.getElementById('pendingPageSize');
		if (prevBtn) {
			prevBtn.addEventListener('click', function () {
				if (pendingCurrentPage > 1) {
					pendingCurrentPage--;
					renderPendingTablePage();
				}
			});
		}
		if (nextBtn) {
			nextBtn.addEventListener('click', function () {
				var n = pendingRowsCache.length;
				var ps = getPendingPageSize();
				var pc = totalPendingPages(n, ps);
				if (pendingCurrentPage < pc) {
					pendingCurrentPage++;
					renderPendingTablePage();
				}
			});
		}
		if (sizeSel) {
			sizeSel.addEventListener('change', function () {
				pendingCurrentPage = 1;
				renderPendingTablePage();
			});
		}
	}

	function showBsModal(el) {
		if (!el) {
			return;
		}
		var $ = window.jQuery;
		/* ihmodule ships Bootstrap 4 (jQuery plugin); BS5 adds getOrCreateInstance. Prefer jQuery when present. */
		if ($ && typeof $.fn.modal === 'function') {
			$(el).modal('show');
			return;
		}
		if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
			if (typeof bootstrap.Modal.getOrCreateInstance === 'function') {
				bootstrap.Modal.getOrCreateInstance(el).show();
				return;
			}
			if (typeof bootstrap.Modal === 'function') {
				try {
					new bootstrap.Modal(el).show();
				} catch (ignore) {
					el.classList.add('show');
					el.style.display = 'block';
					el.removeAttribute('aria-hidden');
				}
				return;
			}
		}
		el.classList.add('show');
		el.style.display = 'block';
		el.removeAttribute('aria-hidden');
	}

	function hideBsModal(el) {
		if (!el) {
			return;
		}
		var $ = window.jQuery;
		if ($ && typeof $.fn.modal === 'function') {
			$(el).modal('hide');
			return;
		}
		if (
			typeof bootstrap !== 'undefined' &&
			bootstrap.Modal &&
			typeof bootstrap.Modal.getInstance === 'function'
		) {
			var inst = bootstrap.Modal.getInstance(el);
			if (inst && typeof inst.hide === 'function') {
				inst.hide();
				return;
			}
		}
		el.classList.remove('show');
		el.style.display = 'none';
		el.setAttribute('aria-hidden', 'true');
	}

	function modalAlert(msg, kind) {
		var box = document.getElementById('modalAlert');
		if (!box) {
			return;
		}
		box.className = 'alert alert-' + (kind || 'info');
		box.textContent = msg;
		box.classList.remove('d-none');
	}

	function clearModalAlert() {
		var box = document.getElementById('modalAlert');
		if (box) {
			box.classList.add('d-none');
			box.textContent = '';
		}
	}

	function pendingUrl() {
		return proxyBase + '/pending';
	}

	function candidatesUrl(caseUuid) {
		return proxyBase + '/candidates?caseUuid=' + encodeURIComponent(caseUuid);
	}

	function forceSyncUrl() {
		return proxyBase + '/force-sync';
	}

	function duplicateReviewSkipUrl() {
		return proxyBase + '/duplicate-review/skip';
	}

	function duplicateReviewAddCandidateUrl() {
		return proxyBase + '/duplicate-review/add-patient-candidate';
	}

	function statisticsUrl() {
		return proxyBase + '/cases/statistics';
	}

	function readOwaspCsrfToken() {
		var names = ['OWASP-CSRFTOKEN', 'OWASP_CSRFTOKEN'];
		var i;
		var el;
		for (i = 0; i < names.length; i++) {
			el = document.querySelector('input[name="' + names[i] + '"]');
			if (el && el.value) {
				return el.value;
			}
			el = document.querySelector('meta[name="' + names[i] + '"]');
			if (el && el.content) {
				return el.content;
			}
		}
		var parts = document.cookie.split(';');
		for (i = 0; i < parts.length; i++) {
			var p = parts[i].trim();
			var eq = p.indexOf('=');
			if (eq < 0) {
				continue;
			}
			var k = p.substring(0, eq);
			if (k === 'OWASP-CSRFTOKEN' || k === 'OWASP_CSRFTOKEN') {
				try {
					return decodeURIComponent(p.substring(eq + 1));
				} catch (ignore) {
					return p.substring(eq + 1);
				}
			}
		}
		return null;
	}

	function buildJsonPostHeaders() {
		var headers = {
			'Content-Type': 'application/json',
			'X-Requested-With': 'XMLHttpRequest'
		};
		var metaTok = document.querySelector('meta[name="_csrf"]');
		var metaHdr = document.querySelector('meta[name="_csrf_header"]');
		if (metaTok && metaHdr && metaTok.content && metaHdr.content) {
			headers[metaHdr.content] = metaTok.content;
		}
		var owasp = readOwaspCsrfToken();
		if (owasp) {
			headers['OWASP-CSRFTOKEN'] = owasp;
			headers['OWASP_CSRFTOKEN'] = owasp;
		}
		return headers;
	}

	function looksLikeHtml(s) {
		return typeof s === 'string' && /^\s*</.test(s);
	}

	function setStatEl(id, n) {
		var el = document.getElementById(id);
		if (el) {
			el.textContent = n != null ? String(n) : '—';
		}
	}

	function loadDupReviewStatistics() {
		fetch(statisticsUrl(), { credentials: 'same-origin' })
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				if (!res.ok) {
					setStatEl('statTotalCases', '—');
					setStatEl('statPending', '—');
					setStatEl('statResolvedLink', '—');
					setStatEl('statResolvedForce', '—');
					return;
				}
				var j = JSON.parse(res.text);
				if (!j || j.error) {
					return;
				}
				setStatEl('statTotalCases', j.totalCases);
				setStatEl('statPending', j.pendingCount);
				setStatEl('statResolvedLink', j.resolvedLinkExistingCount);
				setStatEl('statResolvedForce', j.resolvedForceSendCount);
			})
			.catch(function () {
				setStatEl('statTotalCases', '—');
				setStatEl('statPending', '—');
				setStatEl('statResolvedLink', '—');
				setStatEl('statResolvedForce', '—');
			});
	}

	function loadPendingList() {
		var tbody = document.getElementById('pendingCasesBody');
		var status = document.getElementById('pendingStatus');
		var wrap = document.getElementById('pendingPaginationWrap');
		if (!tbody) {
			return;
		}
		wirePendingPaginationIfNeeded();
		pendingRowsCache = [];
		if (wrap) {
			wrap.style.display = 'none';
		}
		tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted">Loading…</td></tr>';
		if (status) {
			status.textContent = '';
			status.className = 'ms-2 text-muted';
		}
		if (!proxyBase) {
			tbody.innerHTML =
				'<tr><td colspan="9" class="text-danger">Patient exchange API base URL is not configured (IH_DUP_CONFIG.proxyBase).</td></tr>';
			return;
		}
		fetchDupReviewGet(pendingUrl())
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, status: r.status, text: t };
				});
			})
			.then(function (res) {
				if (!res.ok) {
					throw new Error(res.status + ': ' + res.text);
				}
				var rows = JSON.parse(res.text);
				if (!Array.isArray(rows)) {
					throw new Error(rows.error || JSON.stringify(rows));
				}
				pendingRowsCache = rows;
				pendingCurrentPage = 1;
				renderPendingTablePage();
			})
			.catch(function (e) {
				pendingRowsCache = [];
				if (wrap) {
					wrap.style.display = 'none';
				}
				tbody.innerHTML =
					'<tr><td colspan="9" class="text-danger">Failed to load pending cases: ' +
					escapeHtml(String(e.message || e)) +
					'</td></tr>';
			});
	}

	function escapeHtml(s) {
		if (s == null) {
			return '';
		}
		return String(s)
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;');
	}

	function readSourceMetaFromPendingRow(tr) {
		if (!tr || !tr.cells || tr.cells.length < 9) {
			return null;
		}
		return {
			created: tr.cells[0].textContent.trim(),
			patientUuid: tr.cells[1].textContent.trim(),
			name: tr.cells[2].textContent.trim(),
			dob: tr.cells[3].textContent.trim(),
			gender: tr.cells[4].textContent.trim(),
			address: tr.cells[5].textContent.trim(),
			candidateCount: tr.cells[6].textContent.trim(),
			caseUuid: tr.cells[7].textContent.trim(),
			telecom: (tr.dataset && tr.dataset.sourceTelecom ? tr.dataset.sourceTelecom : '').trim()
		};
	}

	function fillModalSourceSummary(meta, caseUuid, sourcePatientUuid) {
		function setText(id, val) {
			var el = document.getElementById(id);
			if (el) {
				el.textContent = val != null && String(val).length ? val : '—';
			}
		}
		if (!meta) {
			setText('modalSrcName', '—');
			setText('modalSrcDob', '—');
			setText('modalSrcGender', '—');
			setText('modalSrcTelecom', '—');
			setText('modalSrcAddress', '—');
			setText('modalSrcCreated', '—');
			setText('modalSrcCandCount', '—');
			setText('modalSourcePatientUuid', sourcePatientUuid || '—');
			setText('modalCaseUuid', caseUuid || '—');
			return;
		}
		setText('modalSrcName', meta.name);
		setText('modalSrcDob', meta.dob);
		setText('modalSrcGender', meta.gender);
		setText('modalSrcTelecom', meta.telecom);
		setText('modalSrcAddress', meta.address);
		setText('modalSrcCreated', meta.created);
		setText('modalSrcCandCount', meta.candidateCount);
		setText('modalSourcePatientUuid', meta.patientUuid || sourcePatientUuid || '');
		setText('modalCaseUuid', meta.caseUuid || caseUuid || '');
	}

	/**
	 * @param {string} caseUuid
	 * @param {string} sourcePatientUuid
	 * @param {object|boolean} sourceMetaOrShowModal  optional row snapshot; or legacy third arg was showModal boolean
	 * @param {boolean} [showModal]
	 */
	function openCandidatesModal(caseUuid, sourcePatientUuid, sourceMetaOrShowModal, showModal) {
		var sourceMeta = null;
		var show = true;
		if (typeof sourceMetaOrShowModal === 'boolean') {
			show = sourceMetaOrShowModal;
		} else if (sourceMetaOrShowModal != null && typeof sourceMetaOrShowModal === 'object') {
			sourceMeta = sourceMetaOrShowModal;
			if (typeof showModal === 'boolean') {
				show = showModal;
			}
		} else if (typeof showModal === 'boolean') {
			show = showModal;
		}

		var modal = document.getElementById('duplicateReviewModal');
		dupReviewModalCaseUuid = caseUuid || '';
		dupReviewModalSourceUuid = sourcePatientUuid || '';
		fillModalSourceSummary(sourceMeta, caseUuid, sourcePatientUuid);
		clearModalAlert();
		wireDupMatchTypeFiltersOnce();
		rebuildDupMatchTypeSelect(document.getElementById('dupMatchTypeFilterOpenmrs'), []);
		rebuildDupMatchTypeSelect(document.getElementById('dupMatchTypeFilterFhir'), []);
		var candBodyOm = document.getElementById('candidatesBodyOpenmrs');
		var candBodyFhir = document.getElementById('candidatesBodyFhir');
		var loadingRow =
			'<tr><td colspan="' + CAND_TABLE_COLSPAN + '" class="text-muted">Loading…</td></tr>';
		if (candBodyOm) {
			candBodyOm.innerHTML = loadingRow;
		}
		if (candBodyFhir) {
			candBodyFhir.innerHTML = loadingRow;
		}
		activateDupCandidateTab('openmrs');

		fetchDupReviewGet(candidatesUrl(caseUuid))
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				if (!res.ok) {
					throw new Error(res.text);
				}
				var parsed = JSON.parse(res.text);
				if (parsed && parsed.error) {
					throw new Error(parsed.error);
				}
				var grouped = normalizeCandidatesPayload(parsed);
				var om = grouped.openmrsCandidates;
				var fh = grouped.fhirCandidates;
				dupCandRawOpenmrs = om && om.length ? om.slice() : [];
				dupCandRawFhir = fh && fh.length ? fh.slice() : [];
				dupCandSourceUuid = sourcePatientUuid || '';
				dupCandMeta = {
					totalStoredCount: grouped.totalStoredCount,
					visibleCount: grouped.visibleCount,
					hiddenCount: grouped.hiddenCount
				};
				updateModalCandidateCount(dupCandMeta);
				setDupTabBadges(dupCandRawOpenmrs.length, dupCandRawFhir.length);
				setupDupTabCandidatesWithMatchTypeFilter('openmrs', dupCandRawOpenmrs, dupCandSourceUuid);
				setupDupTabCandidatesWithMatchTypeFilter('fhir', dupCandRawFhir, dupCandSourceUuid);
				if ((!om || !om.length) && fh && fh.length) {
					activateDupCandidateTab('fhir');
				} else {
					activateDupCandidateTab('openmrs');
				}
				if (show !== false && modal) {
					showBsModal(modal);
				}
			})
			.catch(function (e) {
				dupCandRawOpenmrs = [];
				dupCandRawFhir = [];
				dupCandSourceUuid = '';
				dupCandMeta = { totalStoredCount: 0, visibleCount: 0, hiddenCount: 0 };
				updateModalCandidateCount(dupCandMeta);
				rebuildDupMatchTypeSelect(document.getElementById('dupMatchTypeFilterOpenmrs'), []);
				rebuildDupMatchTypeSelect(document.getElementById('dupMatchTypeFilterFhir'), []);
				var errRow =
					'<tr><td colspan="' +
					CAND_TABLE_COLSPAN +
					'" class="text-danger">' +
					escapeHtml(String(e.message || e)) +
					'</td></tr>';
				if (candBodyOm) {
					candBodyOm.innerHTML = errRow;
				}
				if (candBodyFhir) {
					candBodyFhir.innerHTML = errRow;
				}
				if (show !== false && modal) {
					showBsModal(modal);
				}
			});
	}

	function doAddPatientFromPending(localPatientUuid, caseUuid, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		btn.disabled = true;
		var status = document.getElementById('pendingStatus');
		if (status) {
			status.textContent = 'Register New Patient running…';
		}
		var payload = {
			patientUuid: localPatientUuid,
			resolvedBy: resolvedBy,
			caseUuid: caseUuid || null
		};
		fetch(forceSyncUrl(), {
			method: 'POST',
			credentials: 'same-origin',
			headers: buildJsonPostHeaders(),
			body: JSON.stringify(payload)
		})
			.then(function (r) {
				return r.text().then(function (t) {
					return {
						ok: r.ok,
						status: r.status,
						text: t,
						redirected: r.redirected
					};
				});
			})
			.then(function (res) {
				btn.disabled = false;
				var parsed = null;
				try {
					parsed = JSON.parse(res.text);
				} catch (ignore) {
					/* leave parsed null */
				}
				var htmlBody = looksLikeHtml(res.text);
				var syncOk =
					!res.redirected &&
					res.ok &&
					res.status >= 200 &&
					res.status < 300 &&
					!htmlBody &&
					parsed &&
					typeof parsed === 'object' &&
					!parsed.error;

				if (!syncOk) {
					var errMsg =
						(res.redirected
							? 'Register New Patient was redirected (often CSRF/session). Refresh the page and retry.'
							: null) ||
						(parsed && parsed.error) ||
						(htmlBody
							? 'Register New Patient returned HTML (often CSRF). Refresh the page and retry.'
							: null) ||
						(!res.ok
							? 'Register New Patient HTTP ' + res.status + ': ' + String(res.text).substring(0, 280)
							: null) ||
						'Register New Patient failed — invalid response.';
					if (status) {
						status.textContent = errMsg;
						status.className = 'ms-2 text-danger';
					}
					loadPendingList();
					return;
				}

				var okMsg =
					'Register New Patient: ' +
					String(parsed.message || parsed.status || 'OK').substring(0, 400);
				if (status) {
					status.textContent = okMsg;
					status.className = 'ms-2 text-success';
				}
				window.location.reload();
			})
			.catch(function (e) {
				btn.disabled = false;
				if (status) {
					status.textContent = 'Network error: ' + e.message;
					status.className = 'ms-2 text-danger';
				}
				loadPendingList();
			});
	}

	function doSkipPendingCase(caseUuid, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		if (!caseUuid) {
			alert('Missing case UUID.');
			return;
		}
		btn.disabled = true;
		var status = document.getElementById('pendingStatus');
		if (status) {
			status.textContent = 'Removing…';
		}
		postSkipPendingReviewCase(caseUuid, btn);
	}

	function postAddPatientCandidate(caseUuid, candidateIdStr, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		if (!caseUuid || !candidateIdStr) {
			alert('Missing case or candidate id.');
			return;
		}
		var cid = parseInt(candidateIdStr, 10);
		if (isNaN(cid)) {
			alert('Invalid candidate id.');
			return;
		}
		btn.disabled = true;
		fetch(duplicateReviewAddCandidateUrl(), {
			method: 'POST',
			credentials: 'same-origin',
			headers: buildJsonPostHeaders(),
			body: JSON.stringify({
				caseUuid: caseUuid,
				candidateId: cid,
				resolvedBy: resolvedBy
			})
		})
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				btn.disabled = false;
				var parsed = null;
				try {
					parsed = JSON.parse(res.text);
				} catch (ex) {
					parsed = null;
				}
				if (res.ok && parsed && !parsed.error) {
					window.location.reload();
					return;
				}
				modalAlert(
					parsed
						? (parsed.message || parsed.error || JSON.stringify(parsed))
						: res.text.substring(0, 400),
					res.ok ? 'warning' : 'danger'
				);
				loadPendingList();
			})
			.catch(function (e) {
				btn.disabled = false;
				modalAlert('Network error: ' + e.message, 'danger');
			});
	}

	function postSkipDuplicateReviewCandidate(caseUuid, candidateIdStr, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		if (!caseUuid || !candidateIdStr) {
			alert('Missing case or candidate id.');
			return;
		}
		var cid = parseInt(candidateIdStr, 10);
		if (isNaN(cid)) {
			alert('Invalid candidate id.');
			return;
		}
		btn.disabled = true;
		fetch(duplicateReviewSkipUrl(), {
			method: 'POST',
			credentials: 'same-origin',
			headers: buildJsonPostHeaders(),
			body: JSON.stringify({ caseUuid: caseUuid, candidateId: cid, resolvedBy: resolvedBy })
		})
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				btn.disabled = false;
				var parsed = null;
				try {
					parsed = JSON.parse(res.text);
				} catch (ex) {
					parsed = null;
				}
				if (res.ok && parsed && !parsed.error) {
					clearModalAlert();
					openCandidatesModal(caseUuid, dupReviewModalSourceUuid, null, false);
					loadPendingList();
					return;
				}
				modalAlert(
					parsed ? (parsed.error || JSON.stringify(parsed)) : res.text.substring(0, 400),
					'danger'
				);
			})
			.catch(function (e) {
				btn.disabled = false;
				modalAlert('Network error: ' + e.message, 'danger');
			});
	}

	/** Pending source patient: skip case (no candidateId) — removes row from pending list. */
	function postSkipPendingReviewCase(caseUuid, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		if (!caseUuid) {
			alert('Missing case UUID.');
			return;
		}
		btn.disabled = true;
		fetch(duplicateReviewSkipUrl(), {
			method: 'POST',
			credentials: 'same-origin',
			headers: buildJsonPostHeaders(),
			body: JSON.stringify({ caseUuid: caseUuid, resolvedBy: resolvedBy })
		})
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				btn.disabled = false;
				var parsed = null;
				try {
					parsed = JSON.parse(res.text);
				} catch (ex) {
					parsed = null;
				}
				if (res.ok && parsed && !parsed.error) {
					loadPendingList();
					return;
				}
				var status = document.getElementById('pendingStatus');
				if (status) {
					status.textContent =
						(parsed && parsed.error) || res.text.substring(0, 400) || 'Remove failed';
					status.className = 'ms-2 text-danger';
				}
				loadPendingList();
			})
			.catch(function (e) {
				btn.disabled = false;
				var status = document.getElementById('pendingStatus');
				if (status) {
					status.textContent = 'Network error: ' + e.message;
					status.className = 'ms-2 text-danger';
				}
			});
	}

	document.addEventListener('DOMContentLoaded', function () {
		wireDupMatchTypeFiltersOnce();
		loadPendingList();
		var refresh = document.getElementById('btnRefreshPending');
		if (refresh) {
			refresh.addEventListener('click', loadPendingList);
		}

		var modalEl = document.getElementById('duplicateReviewModal');
		if (modalEl) {
			function onModalHidden() {
				clearModalAlert();
			}
			if (window.jQuery && typeof window.jQuery.fn.modal === 'function') {
				window.jQuery(modalEl).on('hidden.bs.modal', onModalHidden);
			} else {
				modalEl.addEventListener('hidden.bs.modal', onModalHidden);
			}
		}
	});
})();
