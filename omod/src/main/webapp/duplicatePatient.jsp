<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="template/localHeader.jsp"%>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/moduleResources/ihmodule/css/bootstrap.min.css">

<c:if test="${not empty csrfTokenValue}">
<meta name="_csrf" content="<c:out value="${csrfTokenValue}" />" />
<meta name="_csrf_header" content="<c:out value="${csrfHeaderName}" />" />
</c:if>

<div class="container-fluid py-3">
	<h3 class="fw-bold mb-3">MPI duplicate review  pending source patients</h3>
	<%--
	<p class="text-muted">
		List from patient exchange API (<code>PENDING</code> cases).
		Use <strong>Force sync &amp; review</strong> to push the source patient through MCI (skips duplicate deferral), then open candidates.
		Use <strong>View duplicates</strong> to load candidates without syncing.
		In the modal, <strong>Set MPI on local patient</strong> calls <code>POST .../mpi/local</code> on the exchange API for the <em>source</em> local patient UUID.
	</p>
	<p class="small text-muted">
		Proxy base (same-origin, CSRFGuard-exempt <code>/ws/rest/v1/...</code>): <code><c:out value="${patientExchangeProxyBase}" /></code>/{pending|candidates|force-sync|mpi-local}<br/>
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
					<th>Candidates</th>
					<th>Case UUID</th>
					<th style="min-width: 220px;">Actions</th>
				</tr>
			</thead>
			<tbody id="pendingCasesBody">
				<tr><td colspan="8" class="text-center text-muted">Loading…</td></tr>
			</tbody>
		</table>
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
				<p class="small mb-2"><strong>Source patient UUID:</strong> <code id="modalSourcePatientUuid"></code></p>
				<p class="small mb-2"><strong>Case UUID:</strong> <code id="modalCaseUuid"></code></p>
				<div id="modalAlert" class="alert d-none" role="alert"></div>
				<div class="table-responsive">
					<table class="table table-sm table-bordered">
						<thead>
							<tr>
								<th>FHIR id</th>
								<th>MPI value</th>
								<th>Name</th>
								<th>DOB</th>
								<th>Gender</th>
								<th>Telecom</th>
								<th style="min-width: 140px;">Action</th>
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
<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/duplicatePatientReview.js?v=patient-exchange-ws3"></script>

<%@ include file="/WEB-INF/template/footer.jsp"%>
