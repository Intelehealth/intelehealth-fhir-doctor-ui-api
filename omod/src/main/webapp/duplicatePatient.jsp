<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="template/localHeader.jsp"%>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/moduleResources/ihmodule/css/bootstrap.min.css">

<style type="text/css">
	/* Wider modal so FHIR candidate columns (DOB, telecom, address) do not wrap excessively */
	#duplicateReviewModal .modal-dialog.modal-xl {
		max-width: min(1680px, 96vw);
		width: 96vw;
		margin-left: auto;
		margin-right: auto;
	}
</style>

<c:if test="${not empty csrfTokenValue}">
<meta name="_csrf" content="<c:out value="${csrfTokenValue}" />" />
<meta name="_csrf_header" content="<c:out value="${csrfHeaderName}" />" />
</c:if>

<div class="container-fluid py-3">
	<h3 class="fw-bold mb-3">MPI duplicate review  pending source patients</h3>

	<div class="row mb-4 d-none" id="dupReviewStatsRow" aria-hidden="true">
		<div class="col-6 col-md-4 col-lg mb-3">
			<div class="card border-secondary h-100 shadow-sm">
				<div class="card-body py-2 px-3">
					<div class="small text-muted text-uppercase" style="font-size:0.7rem;">Total duplicate reviews</div>
					<div class="h4 mb-0 fw-bold" id="statTotalCases">—</div>
					<div class="small text-muted mt-1" style="font-size:0.75rem;">All rows in review table</div>
				</div>
			</div>
		</div>
		<div class="col-6 col-md-4 col-lg mb-3">
			<div class="card border-warning h-100 shadow-sm">
				<div class="card-body py-2 px-3">
					<div class="small text-muted text-uppercase" style="font-size:0.7rem;">PENDING</div>
					<div class="h4 mb-0 fw-bold text-warning" id="statPending">—</div>
				</div>
			</div>
		</div>
		<div class="col-6 col-md-4 col-lg mb-3">
			<div class="card border-success h-100 shadow-sm">
				<div class="card-body py-2 px-3">
					<div class="small text-muted text-uppercase" style="font-size:0.7rem;">RESOLVED_LINK_EXISTING</div>
					<div class="h4 mb-0 fw-bold text-success" id="statResolvedLink">—</div>
				</div>
			</div>
		</div>
		<div class="col-6 col-md-4 col-lg mb-3">
			<div class="card border-primary h-100 shadow-sm">
				<div class="card-body py-2 px-3">
					<div class="small text-muted text-uppercase" style="font-size:0.7rem;">RESOLVED_FORCE_SEND</div>
					<div class="h4 mb-0 fw-bold text-primary" id="statResolvedForce">—</div>
				</div>
			</div>
		</div>
	</div>
	<%--
	<p class="text-muted">
		List from patient exchange API (<code>PENDING</code> cases).
		Use <strong>Force sync &amp; review</strong> to push the source patient through MCI (skips duplicate deferral), then open candidates.
		Use <strong>View duplicates</strong> to load candidates without syncing.
		In the modal, use <strong>Link And Join Patient</strong> (<code>duplicate-review/add-patient-candidate</code>); <code>mpi-local</code> is deprecated.
	</p>
	<p class="small text-muted">
		Proxy base (same-origin, CSRFGuard-exempt <code>/ws/rest/v1/...</code>): <code><c:out value="${patientExchangeProxyBase}" /></code>/{pending|candidates|force-sync|duplicate-review/*}<br/>
		Upstream URL: edit <code>ihmodule.properties</code> (<code>patientexchange.baseUrl</code>) in the ihmodule API jar, or override at runtime via Administration → Settings → <code><c:out value="${patientExchangeGpKey}" /></code>.
	</p>
--%>
	<div class="mb-3">
		<button type="button" id="btnRefreshPending" class="btn btn-outline-secondary btn-sm">Refresh list</button>
		<span id="pendingStatus" class="ms-2 text-muted"></span>
	</div>

	<div class="table-responsive">
		<table class="table table-sm table-striped table-bordered align-middle" id="pendingCasesTable">
			<thead class="table-light">
				<tr>
					<th>Created</th>
					<th>Local patient UUID</th>
					<th>Name</th>
					<th>DOB</th>
					<th>Gender</th>
					<th>Address (snapshot)</th>
					<th>Candidates</th>
					<th>Case UUID</th>
					<th style="min-width: 280px;">Actions</th>
				</tr>
			</thead>
			<tbody id="pendingCasesBody">
				<tr><td colspan="9" class="text-center text-muted">Loading…</td></tr>
			</tbody>
		</table>
	</div>

	<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 py-2 border-top mt-1"
			id="pendingPaginationWrap" style="display: none;" aria-label="Pending cases pagination">
		<div class="small text-muted" id="pendingPaginationInfo"></div>
		<div class="d-flex flex-wrap align-items-center gap-2">
			<label class="small mb-0 text-muted" for="pendingPageSize">Rows per page</label>
			<select id="pendingPageSize" class="form-control form-control-sm" style="width: auto; min-width: 5rem;">
				<option value="10" selected>10</option>
				<option value="25">25</option>
				<option value="50">50</option>
				<option value="100">100</option>
				<option value="0">All</option>
			</select>
			<nav class="mb-0">
				<ul class="pagination pagination-sm mb-0">
					<li class="page-item" id="pendingPagePrevLi">
						<button type="button" class="page-link" id="pendingPagePrev">Previous</button>
					</li>
					<li class="page-item disabled" id="pendingPageIndicatorLi">
						<span class="page-link py-2 px-3" id="pendingPageIndicator">Page 1 / 1</span>
					</li>
					<li class="page-item" id="pendingPageNextLi">
						<button type="button" class="page-link" id="pendingPageNext">Next</button>
					</li>
				</ul>
			</nav>
		</div>
	</div>
</div>

<!-- Modal: duplicate candidates -->
<div class="modal fade" id="duplicateReviewModal" tabindex="-1" aria-labelledby="duplicateReviewModalLabel" aria-hidden="true">
	<div class="modal-dialog modal-xl modal-dialog-scrollable">
		<div class="modal-content">
			<div class="modal-header">
				<h5 class="modal-title" id="duplicateReviewModalLabel">MPI duplicate candidates</h5>
				<button type="button" class="close" data-dismiss="modal" data-bs-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
			</div>
			<div class="modal-body">
				<div id="modalSourceSummary" class="card card-body bg-light small mb-3 py-2 px-3">
					<div class="fw-semibold mb-2">Source patient (local)</div>
					<dl class="row mb-0 small">
						<dt class="col-sm-3 mb-1 text-muted">Name</dt>
						<dd class="col-sm-9 mb-1" id="modalSrcName">—</dd>
						<dt class="col-sm-3 mb-1 text-muted">DOB</dt>
						<dd class="col-sm-9 mb-1" id="modalSrcDob">—</dd>
						<dt class="col-sm-3 mb-1 text-muted">Gender</dt>
						<dd class="col-sm-9 mb-1" id="modalSrcGender">—</dd>
						<dt class="col-sm-3 mb-1 text-muted">Telecom</dt>
						<dd class="col-sm-9 mb-1" id="modalSrcTelecom">—</dd>
						<dt class="col-sm-3 mb-1 text-muted">Address</dt>
						<dd class="col-sm-9 mb-1 text-break" style="white-space:pre-line" id="modalSrcAddress">—</dd>
						<dt class="col-sm-3 mb-1 text-muted">Created</dt>
						<dd class="col-sm-9 mb-1" id="modalSrcCreated">—</dd>
						<dt class="col-sm-3 mb-1 text-muted">Candidates</dt>
						<dd class="col-sm-9 mb-1" id="modalSrcCandCount">—</dd>
					</dl>
					<p class="mb-0 mt-2 border-top pt-2 text-muted" style="font-size:0.8rem;">
						<strong>IDs (reference):</strong>
						Local patient <code id="modalSourcePatientUuid"></code>
						<span class="mx-1">·</span>
						Case <code id="modalCaseUuid"></code>
					</p>
				</div>
				<div id="modalAlert" class="alert d-none" role="alert"></div>
				<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
					<label class="small mb-0 text-muted" for="dupMatchTypeFilter">Match type filter</label>
					<select id="dupMatchTypeFilter" class="form-control form-control-sm" style="max-width: 280px;" title="Client-side filter only">
						<option value="">All match types</option>
					</select>
				</div>
				<div class="table-responsive">
					<table class="table table-sm table-bordered">
						<thead class="table-light">
							<tr>
								<th style="min-width:10rem;">Name</th>
								<th style="min-width:7rem;">DOB</th>
								<th style="min-width:5rem;">Gender</th>
								<th style="min-width:9rem;">Telecom</th>
								<th style="min-width:12rem;">Address (snapshot)</th>
								<th style="min-width:5rem;">Score</th>
								<th style="min-width:6rem;">Match type</th>
								<th style="min-width:7rem;">Source of patient</th>
								<th style="min-width: 160px;">Actions</th>
							</tr>
						</thead>
						<tbody id="candidatesBody"></tbody>
					</table>
				</div>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-secondary" data-dismiss="modal" data-bs-dismiss="modal">Close</button>
			</div>
		</div>
	</div>
</div>

<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/jquery-3.6.0.min.js"></script>
<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/bootstrap.min.js"></script>

<script type="text/javascript">
	window.IH_DUP_CONFIG = {
		proxyBase: '<c:out value="${patientExchangeProxyBase}" />',
		resolvedBy: '<c:out value="${resolvedByUsername}" />'
	};
</script>
<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/duplicatePatientReview.js?v=patient-exchange-ws21"></script>

<%@ include file="/WEB-INF/template/footer.jsp"%>
