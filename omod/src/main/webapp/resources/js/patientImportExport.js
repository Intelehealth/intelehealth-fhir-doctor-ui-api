(function ($) {
	'use strict';

	function normalizeProxyBase(base) {
		if (!base) {
			return '';
		}
		return base.replace(/\/+$/, '');
	}

	function showAlert($el, kind, message) {
		$el.removeClass('d-none alert-success alert-danger alert-warning alert-info');
		if (!message) {
			$el.addClass('d-none').text('');
			return;
		}
		var cls = kind === 'success' ? 'alert-success' : kind === 'warn' ? 'alert-warning' : 'alert-danger';
		$el.addClass(cls).removeClass('d-none').text(message);
	}

	function parseFilenameFromDisposition(cd) {
		if (!cd) {
			return null;
		}
		var m = /filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?/i.exec(cd);
		return m ? m[1].trim() : null;
	}

	var cfg = window.IH_IMPORT_EXPORT_CONFIG || {};
	var proxyBase = normalizeProxyBase(cfg.proxyBase || '');

	$('#btnImportUpload').on('click', function () {
		var fileInput = document.getElementById('importFile');
		var $status = $('#importStatus');
		var $alert = $('#importAlert');
		var $summary = $('#importSummary');
		var $wrap = $('#importTableWrap');
		var $tbody = $('#importItemsBody');

		if (!fileInput || !fileInput.files || !fileInput.files.length) {
			showAlert($alert, 'err', 'Choose a JSON file to upload.');
			return;
		}

		var fd = new FormData();
		fd.append('file', fileInput.files[0]);

		$status.text('Uploading…');
		showAlert($alert, '', '');
		$summary.addClass('d-none').empty();
		$wrap.addClass('d-none');
		$tbody.empty();

		$.ajax({
			url: proxyBase + '/import-upload',
			type: 'POST',
			data: fd,
			processData: false,
			contentType: false,
			dataType: 'json'
		}).done(function (data) {
			$status.text('');
			if (data.error) {
				showAlert($alert, 'err', data.error);
				return;
			}
			var parts = [];
			parts.push('Total: ' + (data.total != null ? data.total : '?'));
			parts.push('Created: ' + (data.created != null ? data.created : '?'));
			parts.push('Skipped: ' + (data.skipped != null ? data.skipped : '?'));
			parts.push('Failed: ' + (data.failed != null ? data.failed : '?'));
			$summary.removeClass('d-none').html('<strong>Summary:</strong> ' + parts.join(' · '));
			showAlert($alert, data.failed > 0 ? 'warn' : 'success',
				data.failed > 0 ? 'Import finished with failures — see rows below.' : 'Import finished successfully.');

			var items = data.items || [];
			if (items.length) {
				items.forEach(function (row, idx) {
					var tr = $('<tr>');
					tr.append($('<td>').text(idx + 1));
					tr.append($('<td>').append($('<code>').text(row.inputId || '')));
					tr.append($('<td>').append($('<span>').text(row.status || '')));
					tr.append($('<td>').append($('<code>').text(row.createdId || '')));
					tr.append($('<td>').text(row.message || ''));
					$tbody.append(tr);
				});
				$wrap.removeClass('d-none');
			}
		}).fail(function (xhr) {
			$status.text('');
			var msg = 'Request failed (' + xhr.status + ').';
			try {
				var j = xhr.responseJSON;
				if (j && j.error) {
					msg = j.error;
				} else if (xhr.responseText) {
					var parsed = JSON.parse(xhr.responseText);
					if (parsed && parsed.error) {
						msg = parsed.error;
					}
				}
			} catch (e) {
				if (xhr.responseText && xhr.responseText.length < 500) {
					msg = xhr.responseText;
				}
			}
			showAlert($alert, 'err', msg);
		});
	});

	$('#btnExportDownload').on('click', function () {
		var start = $('#exportStartDate').val();
		var end = $('#exportEndDate').val();
		var $status = $('#exportStatus');
		var $alert = $('#exportAlert');
		var $summary = $('#exportSummary');

		if (!start || !end) {
			showAlert($alert, 'err', 'Select both start and end dates.');
			return;
		}
		if (start > end) {
			showAlert($alert, 'err', 'Start date must be on or before end date.');
			return;
		}

		$status.text('Exporting…');
		showAlert($alert, '', '');
		$summary.addClass('d-none').empty();

		var url = proxyBase + '/export-created?startDate=' + encodeURIComponent(start) + '&endDate=' + encodeURIComponent(end);

		fetch(url, { credentials: 'same-origin' }).then(function (res) {
			var cd = res.headers.get('Content-Disposition');
			var xt = res.headers.get('X-Total-Patients');
			var xe = res.headers.get('X-Exported-Patients');
			var xv = res.headers.get('X-Validation-Failed-Patients');
			return res.blob().then(function (blob) {
				return {
					ok: res.ok,
					status: res.status,
					cd: cd,
					xt: xt,
					xe: xe,
					xv: xv,
					blob: blob
				};
			});
		}).then(function (r) {
			$status.text('');
			if (!r.ok) {
				return r.blob.text().then(function (text) {
					var msg = 'Export failed (' + r.status + ').';
					try {
						var j = JSON.parse(text);
						if (j.error) {
							msg = j.error;
						}
					} catch (e2) {
						if (text && text.length < 500) {
							msg = text;
						}
					}
					showAlert($alert, 'err', msg);
				});
			}

			var fname = parseFilenameFromDisposition(r.cd) || ('created-patients-' + start + '-to-' + end + '.json');
			var link = document.createElement('a');
			link.href = URL.createObjectURL(r.blob);
			link.download = fname;
			document.body.appendChild(link);
			link.click();
			link.remove();
			setTimeout(function () {
				URL.revokeObjectURL(link.href);
			}, 2000);

			var hparts = [];
			if (r.xt != null) {
				hparts.push('Total patients (range): ' + r.xt);
			}
			if (r.xe != null) {
				hparts.push('Exported in bundle: ' + r.xe);
			}
			if (r.xv != null) {
				hparts.push('Validation failed (excluded): ' + r.xv);
			}
			if (hparts.length) {
				$summary.removeClass('d-none').html('<strong>Headers:</strong> ' + hparts.join(' · '));
			}
			showAlert($alert, Number(r.xv) > 0 ? 'warn' : 'success',
				Number(r.xv) > 0 ? 'Download started — some patients failed validation and were omitted.' : 'Download started.');
		}).catch(function () {
			$status.text('');
			showAlert($alert, 'err', 'Export request failed.');
		});
	});
})(jQuery);
