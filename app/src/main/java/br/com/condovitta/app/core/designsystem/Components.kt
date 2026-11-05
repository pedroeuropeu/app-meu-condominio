package br.com.condovitta.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.condovitta.app.R

/** Logo Condovitta (versão compacta, sem slogan): texto azul no claro, branco no escuro. */
@Composable
fun BrandLogo(dark: Boolean, modifier: Modifier = Modifier, slogan: Boolean = false) {
    val res = when {
        slogan && dark -> R.drawable.logo_condovitta_escuro
        slogan -> R.drawable.logo_condovitta
        dark -> R.drawable.logo_condovitta_compacta_escuro
        else -> R.drawable.logo_condovitta_compacta
    }
    Image(painterResource(res), contentDescription = "Condovitta", modifier = modifier)
}

/** Emblema da marca (prédios + casa + arco), em vetor. */
@Composable
fun Emblem(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_emblem), contentDescription = null, modifier = modifier.size(size))
}

/** Topo em degradê (marinho → azul-petróleo; âmbar na Portaria), passando por baixo da barra de status. */
@Composable
fun Hero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ext = MaterialTheme.ext
    // ícones da barra de status brancos sobre o degradê; ao sair, voltam ao do tema
    val view = LocalView.current
    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    if (!view.isInEditMode) {
        DisposableEffect(lightTheme) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.isAppearanceLightStatusBars = false
            onDispose { controller?.isAppearanceLightStatusBars = lightTheme }
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(listOf(ext.heroStart, ext.heroEnd)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 20.dp),
        content = content
    )
}

/** Avatar redondo com as iniciais. */
@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier, size: Dp = 46.dp, container: Color = Color.White.copy(alpha = 0.18f), content: Color = Color.White) {
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Text(initials, color = content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Ícone num quadrado arredondado com fundo suave da cor (padrão das listas e grades do protótipo). */
@Composable
fun IconBubble(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.52f)) }
}

/** Selo de estado (Esperado, No condomínio, Em construção...). */
@Composable
fun StatusBadge(label: String, color: Color, container: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Cartão branco com borda fina (superfície padrão do protótipo). */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.ext.card,
        border = BorderStroke(1.dp, MaterialTheme.ext.cardBorder)
    ) { Column(content = content) }
}

/** Título de seção ("Ações rápidas", "Conta"...). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(top = 18.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ext.ink)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted)
    }
}

/** Linha de menu: ícone colorido, título, subtítulo e a seta. */
@Composable
fun MenuRow(icon: ImageVector, color: Color, title: String, subtitle: String? = null, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconBubble(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.ext.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing() else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.ext.faint)
    }
}

/** Barra de topo das telas internas (com voltar). */
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar") } },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/**
 * Estado "Em construção" honesto: o que a tela vai fazer e o selo. Igual ao soonBody() do protótipo.
 */
@Composable
fun SoonContent(title: String, icon: ImageVector, color: Color, description: String, modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBubble(icon, color, size = 76.dp)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.ext.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.ext.muted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp))
        Spacer(Modifier.height(20.dp))
        StatusBadge("Em construção", MaterialTheme.ext.info, MaterialTheme.ext.infoContainer, icon = Icons.Rounded.Construction)
        Spacer(Modifier.height(14.dp))
        Text(
            "Esta é uma das próximas partes do app. Estamos fazendo por módulos.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ext.faint, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp)
        )
    }
}
