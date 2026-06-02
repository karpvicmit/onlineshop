function updateStrength(val) {
    const wrap  = document.getElementById('strengthWrap');
    const fill  = document.getElementById('strengthFill');
    const label = document.getElementById('strengthLabel');
    if (!val) { wrap.style.display = 'none'; return; }

    wrap.style.display = 'block';
    let score = 0;
    if (val.length >= 8)              score++;
    if (/[A-Z]/.test(val))            score++;
    if (/[0-9]/.test(val))            score++;
    if (/[^A-Za-z0-9]/.test(val))     score++;

    const levels = [
        { pct: '15%',  bg: '#e05c5c', text: 'Sehr schwach' },
        { pct: '35%',  bg: '#e0975c', text: 'Schwach'      },
        { pct: '60%',  bg: '#e0c55c', text: 'Mittel'       },
        { pct: '85%',  bg: '#8dcc5c', text: 'Stark'        },
        { pct: '100%', bg: '#5cb88a', text: 'Sehr stark'   },
    ];
    const lvl = levels[score] || levels[0];
    fill.style.width      = lvl.pct;
    fill.style.background = lvl.bg;
    label.textContent     = lvl.text;
    label.style.color     = lvl.bg;
}