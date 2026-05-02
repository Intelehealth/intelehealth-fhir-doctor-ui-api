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
	<h3 class="fw-bold mb-3">Patient import &amp; export</h3>
	 <%--
	<p class="text-muted">
		Uses the patient data exchange API: multipart upload to <code>POST /patient/import/upload</code>
		(FHIR R4 <strong>Patient</strong> or <strong>Bundle</strong> JSON), and date-range export from
		<code>GET …/patient/export/created</code> (<code>startDate</code> / <code>endDate</code> as <code>yyyy-MM-dd</code>).
	</p>--%>
	<p class="small text-muted">
		
		Upstream base URL: Administration → Settings → <code><c:out value="${patientExchangeGpKey}" /></code>
		(or <code>patientexchange.baseUrl</code> in <code>ihmodule.properties</code>).
	</p>

	<div class="row g-4">
		<div class="col-lg-6">
			<div class="card border-secondary">
				<div class="card-header bg-light fw-semibold">Import patients (JSON upload)</div>
				<div class="card-body">
					<p class="small text-muted mb-3">
						Upload a file containing one FHIR Patient resource or a Bundle whose entries include Patients.
						The API returns counts (<code>total</code>, <code>created</code>, <code>skipped</code>, <code>failed</code>) and per-row <code>items</code>
						with <code>inputId</code>, <code>status</code> (<code>CREATED</code> | <code>SKIPPED</code> | <code>FAILED</code>), <code>message</code>, and optional <code>createdId</code>.
					</p>
					<div class="mb-3">
						<label for="importFile" class="form-label">FHIR JSON file</label>
						<input type="file" class="form-control form-control-sm" id="importFile" accept=".json,application/json" />
					</div>
					<button type="button" id="btnImportUpload" class="btn btn-primary btn-sm">Upload &amp; import</button>
					<span id="importStatus" class="ms-2 text-muted small"></span>
					<div id="importAlert" class="alert d-none mt-3 mb-0" role="alert"></div>
					<div id="importSummary" class="small mt-3 d-none"></div>
					<div class="table-responsive mt-3 d-none" id="importTableWrap">
						<table class="table table-sm table-striped table-bordered align-middle mb-0">
							<thead class="table-light">
								<tr>
									<th>#</th>
									<th>Input id</th>
									<th>Status</th>
									<th>Created id</th>
									<th>Message</th>
								</tr>
							</thead>
							<tbody id="importItemsBody"></tbody>
						</table>
					</div>
				</div>
			</div>
		</div>

		<div class="col-lg-6">
			<div class="card border-secondary">
				<div class="card-header bg-light fw-semibold">Export created patients (JSON download)</div>
				<div class="card-body">
					<p class="small text-muted mb-3">
						Patients whose OpenMRS <code>person.date_created</code> falls within the inclusive date range are exported as a FHIR
						<code>collection</code> Bundle. Response headers include <code>X-Total-Patients</code>, <code>X-Exported-Patients</code>,
						and <code>X-Validation-Failed-Patients</code>; the body is attachment JSON named <code>created-patients-&lt;start&gt;-to-&lt;end&gt;.json</code>.
					</p>
					<div class="row g-2 mb-3">
						<div class="col-md-6">
							<label for="exportStartDate" class="form-label">Start date</label>
							<input type="date" class="form-control form-control-sm" id="exportStartDate" />
						</div>
						<div class="col-md-6">
							<label for="exportEndDate" class="form-label">End date</label>
							<input type="date" class="form-control form-control-sm" id="exportEndDate" />
						</div>
					</div>
					<button type="button" id="btnExportDownload" class="btn btn-outline-primary btn-sm">Download export</button>
					<span id="exportStatus" class="ms-2 text-muted small"></span>
					<div id="exportAlert" class="alert d-none mt-3 mb-0" role="alert"></div>
					<div id="exportSummary" class="small mt-3 text-muted d-none"></div>
				</div>
			</div>
		</div>
	</div>
</div>

<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/jquery-3.6.0.min.js"></script>
<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/bootstrap.min.js"></script>

<script type="text/javascript">
	window.IH_IMPORT_EXPORT_CONFIG = {
		proxyBase: '<c:out value="${patientExchangeProxyBase}" />'
	};
</script>
<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/patientImportExport.js?v=1"></script>

<%@ include file="/WEB-INF/template/footer.jsp"%>
