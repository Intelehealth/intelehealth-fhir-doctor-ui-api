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

	function mpiLocalUrl() {
		return proxyBase + '/mpi-local';
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

	function loadPendingList() {
		var tbody = document.getElementById('pendingCasesBody');
		var status = document.getElementById('pendingStatus');
		if (!tbody) {
			return;
		}
		tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">Loading…</td></tr>';
		if (status) {
			status.textContent = '';
			status.className = 'ms-2 text-muted';
		}
		fetch(pendingUrl(), { credentials: 'same-origin' })
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
				tbody.innerHTML = '';
				if (!rows.length) {
					tbody.innerHTML =
						'<tr><td colspan="8" class="text-center text-muted">No pending duplicate-review cases.</td></tr>';
					return;
				}
				rows.forEach(function (row) {
					var tr = document.createElement('tr');
					var name =
						[row.sourceGiven || '', row.sourceFamily || ''].join(' ').trim() || '—';
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
						'<td>' +
						String(row.candidateCount != null ? row.candidateCount : '') +
						'</td>' +
						'<td><small>' +
						escapeHtml(row.caseUuid || '') +
						'</small></td>' +
						'<td>' +
						'<button type="button" class="btn btn-sm btn-outline-primary me-1 btn-view-dup">View duplicates</button>' +
						'<button type="button" class="btn btn-sm btn-primary btn-force-sync">Force sync &amp; review</button>' +
						'</td>';
					tr.dataset.caseUuid = row.caseUuid || '';
					tr.dataset.localPatientUuid = row.localPatientUuid || '';
					tbody.appendChild(tr);
				});

				Array.prototype.forEach.call(tbody.querySelectorAll('.btn-view-dup'), function (btn) {
					btn.addEventListener('click', function () {
						var tr = btn.closest('tr');
						openCandidatesModal(tr.dataset.caseUuid, tr.dataset.localPatientUuid);
					});
				});
				Array.prototype.forEach.call(tbody.querySelectorAll('.btn-force-sync'), function (btn) {
					btn.addEventListener('click', function () {
						var tr = btn.closest('tr');
						doForceSyncThenReview(tr.dataset.localPatientUuid, tr.dataset.caseUuid, btn);
					});
				});
			})
			.catch(function (e) {
				tbody.innerHTML =
					'<tr><td colspan="8" class="text-danger">Failed to load pending cases: ' +
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

	function openCandidatesModal(caseUuid, sourcePatientUuid, showModal) {
		var modal = document.getElementById('duplicateReviewModal');
		document.getElementById('modalSourcePatientUuid').textContent = sourcePatientUuid || '';
		document.getElementById('modalCaseUuid').textContent = caseUuid || '';
		clearModalAlert();
		var candBody = document.getElementById('candidatesBody');
		candBody.innerHTML = '<tr><td colspan="7" class="text-muted">Loading…</td></tr>';

		fetch(candidatesUrl(caseUuid), { credentials: 'same-origin' })
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				if (!res.ok) {
					throw new Error(res.text);
				}
				var list = JSON.parse(res.text);
				if (!Array.isArray(list)) {
					throw new Error(list.error || JSON.stringify(list));
				}
				candBody.innerHTML = '';
				if (!list.length) {
					candBody.innerHTML =
						'<tr><td colspan="7" class="text-muted">No candidates returned.</td></tr>';
				} else {
					list.forEach(function (c) {
						var nm = [c.candidateGiven || '', c.candidateFamily || ''].join(' ').trim() || '—';
						var tr = document.createElement('tr');
						tr.innerHTML =
							'<td><code>' +
							escapeHtml(c.fhirPatientLogicalId || '') +
							'</code></td>' +
							'<td><input type="text" class="form-control form-control-sm mpi-val-input" value="' +
							escapeHtml(c.mpiIdentifierValue || '') +
							'" placeholder="MPI id"/></td>' +
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
							'<td><button type="button" class="btn btn-sm btn-success btn-set-mpi">Set MPI</button></td>';
						tr.dataset.fhirLogicalId = c.fhirPatientLogicalId || '';
						candBody.appendChild(tr);
					});

					Array.prototype.forEach.call(candBody.querySelectorAll('.btn-set-mpi'), function (btn) {
						btn.addEventListener('click', function () {
							var tr = btn.closest('tr');
							var mpiInput = tr.querySelector('.mpi-val-input');
							var mpiVal = mpiInput ? mpiInput.value.trim() : '';
							postSetMpi(sourcePatientUuid, mpiVal, tr.dataset.fhirLogicalId, btn);
						});
					});
				}
				if (showModal !== false && modal) {
					showBsModal(modal);
				}
			})
			.catch(function (e) {
				candBody.innerHTML =
					'<tr><td colspan="7" class="text-danger">' + escapeHtml(String(e.message || e)) + '</td></tr>';
				if (showModal !== false && modal) {
					showBsModal(modal);
				}
			});
	}

	function doForceSyncThenReview(localPatientUuid, caseUuid, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		btn.disabled = true;
		var status = document.getElementById('pendingStatus');
		if (status) {
			status.textContent = 'Force sync running…';
		}
		var payload = JSON.stringify({
			patientUuid: localPatientUuid,
			resolvedBy: resolvedBy
		});
		fetch(forceSyncUrl(), {
			method: 'POST',
			credentials: 'same-origin',
			headers: buildJsonPostHeaders(),
			body: payload
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
							? 'Force sync was redirected (often CSRF/session). Refresh the page and retry.'
							: null) ||
						(parsed && parsed.error) ||
						(htmlBody ? 'Force sync returned HTML (often CSRF). Refresh the page and retry.' : null) ||
						(!res.ok ? 'Force sync HTTP ' + res.status + ': ' + String(res.text).substring(0, 280) : null) ||
						'Force sync failed — invalid response.';
					if (status) {
						status.textContent = errMsg;
						status.className = 'ms-2 text-danger';
					}
					loadPendingList();
					return;
				}

				var okMsg =
					'Force sync: ' +
					String(parsed.message || parsed.status || 'OK').substring(0, 400);
				if (status) {
					status.textContent = okMsg;
					status.className = 'ms-2 text-success';
				}
				modalAlert(okMsg, 'success');
				openCandidatesModal(caseUuid, localPatientUuid, true);
				loadPendingList();
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

	function postSetMpi(sourcePatientUuid, mpiIdentifierValue, chosenFhirPatientLogicalId, btn) {
		if (!resolvedBy) {
			alert('resolvedBy is empty — log in to OpenMRS.');
			return;
		}
		if (!mpiIdentifierValue) {
			alert('Enter or confirm MPI identifier value.');
			return;
		}
		btn.disabled = true;
		var payload = {
			patientUuid: sourcePatientUuid,
			mpiIdentifierValue: mpiIdentifierValue,
			resolvedBy: resolvedBy
		};
		if (chosenFhirPatientLogicalId) {
			payload.chosenFhirPatientLogicalId = chosenFhirPatientLogicalId;
		}
		fetch(mpiLocalUrl(), {
			method: 'POST',
			credentials: 'same-origin',
			headers: buildJsonPostHeaders(),
			body: JSON.stringify(payload)
		})
			.then(function (r) {
				return r.text().then(function (t) {
					return { ok: r.ok, text: t };
				});
			})
			.then(function (res) {
				btn.disabled = false;
				try {
					var j = JSON.parse(res.text);
					modalAlert(
						j.message || j.error || JSON.stringify(j),
						res.ok && j.status !== 'skipped' ? 'success' : 'warning'
					);
				} catch (ex) {
					modalAlert(res.text.substring(0, 400), res.ok ? 'success' : 'danger');
				}
				loadPendingList();
			})
			.catch(function (e) {
				btn.disabled = false;
				modalAlert('Network error: ' + e.message, 'danger');
			});
	}

	document.addEventListener('DOMContentLoaded', function () {
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
