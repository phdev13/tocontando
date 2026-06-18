import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const INGEST_URL = process.env.INGEST_URL || 'http://127.0.0.1:8787/api/v1/performance/runs';
const TOKEN = process.env.PERFORMANCE_INGEST_TOKEN;

if (!TOKEN) {
    console.error('Missing PERFORMANCE_INGEST_TOKEN environment variable.');
    process.exit(1);
}

const benchmarkDir = path.join(process.cwd(), 'macrobenchmark/build/outputs/connected_android_test_additional_output/benchmark/connected');

if (!fs.existsSync(benchmarkDir)) {
    console.error('Benchmark output directory not found:', benchmarkDir);
    process.exit(1);
}

const files = fs.readdirSync(benchmarkDir);
const jsonFile = files.find(f => f.endsWith('.json'));

if (!jsonFile) {
    console.error('Benchmark JSON not found.');
    process.exit(1);
}

const resultPath = path.join(benchmarkDir, jsonFile);
const resultData = JSON.parse(fs.readFileSync(resultPath, 'utf8'));

// Extract metadata
const context = resultData.context;
const buildType = context.build.type || 'benchmark';
const runId = crypto.randomUUID();

// Hash the file to prevent duplicates
const fileHash = crypto.createHash('sha256').update(fs.readFileSync(resultPath)).digest('hex');

const runPayload = {
    id: runId,
    source: 'MACROBENCHMARK',
    app_version: context.build.versionName || '1.0',
    version_code: context.build.versionCode || 1,
    commit_sha: process.env.GITHUB_SHA || null,
    branch: process.env.GITHUB_REF_NAME || null,
    build_type: buildType,
    benchmark_name: 'MacrobenchmarkSuite',
    compilation_mode: 'baseline_profile',
    iterations: 5,
    device_model: context.build.model || 'Unknown',
    device_manufacturer: context.build.brand || 'Unknown',
    android_version: context.build.version.release || 'Unknown',
    api_level: context.build.version.sdk || 35,
    refresh_rate: null,
    memory_available_mb: null,
    battery_level: null,
    thermal_status: null,
    status: 'COMPLETED',
    payload_hash: fileHash
};

async function upload() {
    try {
        console.log(`Uploading run ${runId}...`);
        const runRes = await fetch(INGEST_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${TOKEN}` },
            body: JSON.stringify(runPayload)
        });

        if (!runRes.ok) {
            const err = await runRes.json();
            if (err.code === 'DUPLICATE') {
                console.log('Run already uploaded (duplicate payload).');
                process.exit(0);
            }
            throw new Error(`Failed to upload run: ${JSON.stringify(err)}`);
        }

        const metrics = [];
        for (const benchmark of resultData.benchmarks) {
            const name = benchmark.name;
            const metricsObj = benchmark.metrics;
            for (const [metricKey, metricValue] of Object.entries(metricsObj)) {
                if (metricValue.runs && metricValue.runs.length > 0) {
                    const sorted = [...metricValue.runs].sort((a, b) => a - b);
                    const min = sorted[0];
                    const max = sorted[sorted.length - 1];
                    const median = sorted[Math.floor(sorted.length / 2)];
                    
                    // Simple metrics insertion for min/max/median
                    metrics.push({ id: crypto.randomUUID(), metric_name: `${name}_${metricKey}`, percentile: 'median', value: median, unit: 'ms' });
                    metrics.push({ id: crypto.randomUUID(), metric_name: `${name}_${metricKey}`, percentile: 'min', value: min, unit: 'ms' });
                    metrics.push({ id: crypto.randomUUID(), metric_name: `${name}_${metricKey}`, percentile: 'max', value: max, unit: 'ms' });
                }
            }
        }

        if (metrics.length > 0) {
            console.log(`Uploading ${metrics.length} metrics...`);
            const metricsRes = await fetch(`${INGEST_URL}/${runId}/metrics`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${TOKEN}` },
                body: JSON.stringify({ metrics })
            });
            if (!metricsRes.ok) throw new Error('Failed to upload metrics');
        }

        // Upload Perfetto Traces
        const traces = files.filter(f => f.endsWith('.perfetto-trace'));
        for (const trace of traces) {
            const tracePath = path.join(benchmarkDir, trace);
            const buffer = fs.readFileSync(tracePath);
            const traceHash = crypto.createHash('sha256').update(buffer).digest('hex');

            const formData = new FormData();
            formData.append('file', new Blob([buffer]), trace);
            formData.append('artifact_type', 'PERFETTO_TRACE');
            formData.append('id', crypto.randomUUID());
            formData.append('sha256', traceHash);

            console.log(`Uploading trace ${trace}...`);
            const traceRes = await fetch(`${INGEST_URL}/${runId}/artifacts`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${TOKEN}` },
                body: formData
            });
            if (!traceRes.ok) throw new Error(`Failed to upload trace ${trace}`);
        }

        console.log('Upload completed successfully!');
    } catch (e) {
        console.error(e);
        process.exit(1);
    }
}

upload();
